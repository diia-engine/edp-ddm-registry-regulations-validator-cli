/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.registry.regulation.validation.cli;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.deliveredtechnologies.rulebook.model.RuleBook;
import com.deliveredtechnologies.rulebook.model.runner.RuleBookRunner;
import com.epam.digital.data.platform.registry.regulation.validation.cli.model.RegulationFiles;
import com.epam.digital.data.platform.registry.regulation.validation.cli.support.CommandLineArg;
import com.epam.digital.data.platform.registry.regulation.validation.cli.support.CommandLineArgsParser;
import com.epam.digital.data.platform.registry.regulation.validation.cli.support.CommandLineOptionsConverter;
import com.epam.digital.data.platform.registry.regulation.validation.cli.support.SystemExit;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.RegulationValidatorFactory;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationError;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class RegulationValidationCommandLineRunnerTest {

  @Autowired
  private ResourceLoader resourceLoader;

  private SystemExit systemExit;
  private CommandLineArgsParser commandLineArgsParser;
  private CommandLineOptionsConverter commandLineOptionsConverter;
  private RegulationValidationCommandLineRunner validationRunner;

  @BeforeEach
  void setUp() {
    this.systemExit = mock(SystemExit.class);
    this.commandLineArgsParser = mock(CommandLineArgsParser.class);
    this.commandLineOptionsConverter = mock(CommandLineOptionsConverter.class);
  }

  @Test
  void shouldDisplayHelpIfSpecified() throws ParseException {
    var options = new Options();
    options.addOption(Option.builder().longOpt("help").build());
    when(commandLineArgsParser.parse("--help")).thenReturn(options);

    validationRunner = newValidationRunner(resourceLoader, commandLineArgsParser,
        commandLineOptionsConverter, systemExit);

    validationRunner.run("--help");

    Mockito.verify(commandLineArgsParser, times(1)).printHelp();
    Mockito.verify(systemExit, times(1)).complete();
  }

  @Test
  void shouldPassIfNoRegulationFilesSpecified() {
    validationRunner = newValidationRunner(resourceLoader, new CommandLineArgsParser(),
        new CommandLineOptionsConverter(), systemExit);

    validationRunner.run("--bp-auth=");

    Mockito.verify(systemExit, times(1)).complete();
  }

  @Test
  void shouldCompleteIfNoFilesPassed() throws ParseException {
    var emptyOptions = new Options();
    when(commandLineArgsParser.parse()).thenReturn(emptyOptions);
    when(commandLineOptionsConverter.convert(emptyOptions)).thenReturn(emptyRegulationFiles());

    validationRunner = newValidationRunner(resourceLoader, commandLineArgsParser,
        commandLineOptionsConverter, systemExit);

    validationRunner.run();

    Mockito.verify(systemExit, times(1)).complete();
  }

  @Test
  void shouldDisplayHelpIfUnrecognizedOptionPassed() throws ParseException {
    when(commandLineArgsParser.parse("-unrecognized")).thenThrow(ParseException.class);

    validationRunner = newValidationRunner(resourceLoader, commandLineArgsParser,
        commandLineOptionsConverter, systemExit);

    validationRunner.run("-unrecognized");

    Mockito.verify(commandLineArgsParser, times(1)).printHelp();
    Mockito.verify(systemExit, times(1)).systemError();
  }

  @Test
  void shouldPassWithoutValidationErrorsLogged() {
    validationRunner = newValidationRunner(resourceLoader, new CommandLineArgsParser(),
        new CommandLineOptionsConverter(), systemExit);

    validationRunner.run(correctRegistryRegulations());

    Mockito.verify(systemExit, times(1)).complete();
  }

  @Test
  void shouldPassEmptyRegulationsWithoutValidationErrorsLogged() {
    validationRunner = newValidationRunner(resourceLoader, new CommandLineArgsParser(),
        new CommandLineOptionsConverter(), systemExit);

    validationRunner.run(emptyRegistryRegulations());

    Mockito.verify(systemExit, times(1)).complete();
  }

  @Test
  void shouldFailBpmnDueToDuplicatedProcessId() {
    validationRunner = newValidationRunner(resourceLoader, new CommandLineArgsParser(), new CommandLineOptionsConverter(), systemExit);

    validationRunner.run(correctRegistryRegulations());
    validationRunner.run(argOf(CommandLineArg.BPMN,
        testResourcePathOf("registry-regulation/broken/test-duplicated-process-id-bp-1.bpmn"),
        testResourcePathOf("registry-regulation/broken/test-duplicated-process-id-bp-2.bpmn"))
    );

    Mockito.verify(systemExit, times(1)).validationFailure();
  }

  @Test
  void shouldFailBpAuthDueToDuplicates() {
    validationRunner = newValidationRunner(resourceLoader, new CommandLineArgsParser(),
        new CommandLineOptionsConverter(), systemExit);

    validationRunner.run(argOf(CommandLineArg.BP_AUTH,
        testResourcePathOf("registry-regulation/broken/bp-auth-duplicates.yml")));

    Mockito.verify(systemExit, times(1)).validationFailure();
  }

  @Test
  void shouldFailBpTrembitaDueToDuplicates() {
    validationRunner = newValidationRunner(resourceLoader, new CommandLineArgsParser(),
        new CommandLineOptionsConverter(), systemExit);

    validationRunner.run(argOf(CommandLineArg.BP_TREMBITA,
        testResourcePathOf("registry-regulation/broken/bp-trembita-duplicates.yml")));

    Mockito.verify(systemExit, times(1)).validationFailure();
  }

  @Test
  void shouldFailGlobalVarsDueToUnknownThemeFile() {
    validationRunner = newValidationRunner(resourceLoader, new CommandLineArgsParser(),
        new CommandLineOptionsConverter(), systemExit);

    validationRunner.run(argOf(CommandLineArg.GLOBAL_VARS,
        testResourcePathOf("registry-regulation/broken/global-vars-themeFile-broken.yml")));

    Mockito.verify(systemExit, times(1)).validationFailure();
  }

  @Test
  void shouldFailDatafactorySettingYamlFileDueToInvalidParams() {
    validationRunner = newValidationRunner(resourceLoader, new CommandLineArgsParser(),
            new CommandLineOptionsConverter(), systemExit);
    validationRunner.run(argOf(CommandLineArg.DATAFACTORY_SETTINGS,
            testResourcePathOf("registry-regulation/broken/settings.yaml")));

    Mockito.verify(systemExit, times(1)).validationFailure();
  }

  @Test
  void shouldFailRegistrySettingsYamlFileDueToInvalidParams() {
    validationRunner = newValidationRunner(resourceLoader, new CommandLineArgsParser(),
            new CommandLineOptionsConverter(), systemExit);
    validationRunner.run(argOf(CommandLineArg.REGISTRY_SETTINGS,
            testResourcePathOf("registry-regulation/broken/registry-settings-long-title.yaml")));

    Mockito.verify(systemExit, times(1)).validationFailure();
  }

  @Test
  void shouldFailMainLiquibaseFileDueToInvalidParams() {
    validationRunner = newValidationRunner(resourceLoader, new CommandLineArgsParser(),
            new CommandLineOptionsConverter(), systemExit);
    validationRunner.run(argOf(CommandLineArg.LIQUIBASE,
            testResourcePathOf("registry-regulation/broken/main-liquibase/test-main-liquibase.xml")));

    Mockito.verify(systemExit, times(1)).validationFailure();
  }

  @Test
  void shouldFailDiiaNotificationDueToInvalidParams() {
    validationRunner = newValidationRunner(resourceLoader, new CommandLineArgsParser(),
        new CommandLineOptionsConverter(), systemExit);
    validationRunner.run(argOf(CommandLineArg.DIIA_NOTIFICATION_TEMPLATE,
        testResourcePathOf("registry-regulation/broken/diia")));

    Mockito.verify(systemExit, times(1)).validationFailure();
  }

  @Test
  void shouldFailDiiaNotificationDueToTemplateFolderIsNotDirectory() {
    validationRunner = newValidationRunner(resourceLoader, new CommandLineArgsParser(),
        new CommandLineOptionsConverter(), systemExit);
    validationRunner.run(argOf(CommandLineArg.DIIA_NOTIFICATION_TEMPLATE,
        testResourcePathOf("registry-regulation/correct/global-vars.yml")));

    Mockito.verify(systemExit, times(1)).validationFailure();
  }

  @Test
  void shouldPassIfDirectoryIsNotExists() {
    validationRunner = newValidationRunner(resourceLoader, new CommandLineArgsParser(),
        new CommandLineOptionsConverter(), systemExit);
    validationRunner.run(argOf(CommandLineArg.DIIA_NOTIFICATION_TEMPLATE,
        "registry-regulation/correct/absent_directory"));

    Mockito.verify(systemExit, times(1)).complete();
  }

  private RegulationValidationCommandLineRunner newValidationRunner(ResourceLoader resourceLoader,
      CommandLineArgsParser commandLineArgsParser,
      CommandLineOptionsConverter commandLineOptionsConverter,
      SystemExit systemExit) {
    return new RegulationValidationCommandLineRunner(
        new RegulationValidatorFactory(resourceLoader, new YAMLMapper(), new JsonMapper(),
                settingsYamlRuleBook(), mainLiquibaseRuleBook()),
        commandLineArgsParser, commandLineOptionsConverter, systemExit
    );
  }

  private RuleBook<Set<ValidationError>> settingsYamlRuleBook() {
    return getRuleBookRunner(
            "com.epam.digital.data.platform.registry.regulation.validation.cli.validator.datasettings.rules");
  }

  private RuleBook<Set<ValidationError>> mainLiquibaseRuleBook() {
    return getRuleBookRunner(
            "com.epam.digital.data.platform.registry.regulation.validation.cli.validator.mainliquibase.rules");
  }

  @SuppressWarnings("unchecked")
  private RuleBook<Set<ValidationError>> getRuleBookRunner(String rulePackage)  {
    var springAwareRuleBookRunner = new RuleBookRunner(rulePackage);
    springAwareRuleBookRunner.setDefaultResult(Sets.newHashSet());
    return springAwareRuleBookRunner;
  }

  private String[] correctRegistryRegulations() {
    return new String[]{
        argOf(CommandLineArg.GLOBAL_VARS, testResourcePathOf("registry-regulation/correct/global-vars.yml")),
        argOf(CommandLineArg.BP_AUTH, testResourcePathOf("registry-regulation/correct/bp-auth.yml")),
        argOf(CommandLineArg.BP_TREMBITA, testResourcePathOf("registry-regulation/correct/bp-trembita.yml")),
        argOf(CommandLineArg.BP_TREMBITA_CONFIG, testResourcePathOf("registry-regulation/correct/configuration.yml")),
        argOf(CommandLineArg.ROLES, testResourcePathOf("registry-regulation/correct/roles.yml")),
        argOf(CommandLineArg.BPMN,
                testResourcePathOf("registry-regulation/correct/process.bpmn"),
                testResourcePathOf("registry-regulation/correct/trembita-process.bpmn")),
        argOf(CommandLineArg.DMN, testResourcePathOf("registry-regulation/correct/rule.dmn")),
        argOf(CommandLineArg.FORMS, testResourcePathOf("registry-regulation/correct/ui-form.json")),
        argOf(CommandLineArg.DATAFACTORY_SETTINGS, testResourcePathOf("registry-regulation/correct/settings.yaml")),
        argOf(CommandLineArg.LIQUIBASE, testResourcePathOf("registry-regulation/correct/test-main-liquibase.xml")),
        argOf(CommandLineArg.DIIA_NOTIFICATION_TEMPLATE, testResourcePathOf("registry-regulation/correct/diia"))
    };
  }

  private String[] emptyRegistryRegulations() {
    return new String[]{
        argOf(CommandLineArg.BP_AUTH, testResourcePathOf("registry-regulation/empty/bp-auth-empty.yml")),
        argOf(CommandLineArg.BP_TREMBITA, testResourcePathOf("registry-regulation/empty/bp-trembita-empty.yml")),
        argOf(CommandLineArg.ROLES, testResourcePathOf("registry-regulation/empty/roles-empty.yml"))
    };
  }

  private String argOf(CommandLineArg arg, Path... resourcePath) {
    return String.format("--%s=%s",
        arg.getArgOptionName(),
        Arrays.stream(resourcePath).map(Path::toString).collect(Collectors.joining(",")));
  }

  private String argOf(CommandLineArg arg, String... resourcePath) {
    return String.format("--%s=%s",
        arg.getArgOptionName(),
        String.join(",", resourcePath));
  }

  @SneakyThrows
  private Path testResourcePathOf(String resource) {
    var classLoader = getClass().getClassLoader();
    return Paths.get(classLoader.getResource(resource).toURI());
  }

  private RegulationFiles emptyRegulationFiles() {
    return RegulationFiles.builder().build();
  }
}