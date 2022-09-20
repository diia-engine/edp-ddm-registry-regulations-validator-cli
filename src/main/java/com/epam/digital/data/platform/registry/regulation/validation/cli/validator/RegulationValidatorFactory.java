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

package com.epam.digital.data.platform.registry.regulation.validation.cli.validator;

import com.deliveredtechnologies.rulebook.model.RuleBook;
import com.epam.digital.data.platform.registry.regulation.validation.cli.model.BpAuthConfiguration;
import com.epam.digital.data.platform.registry.regulation.validation.cli.model.BpTrembitaConfiguration;
import com.epam.digital.data.platform.registry.regulation.validation.cli.model.RegulationFileType;
import com.epam.digital.data.platform.registry.regulation.validation.cli.model.RegulationFiles;
import com.epam.digital.data.platform.registry.regulation.validation.cli.support.RegulationConfigurationLoader;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.bpmn.BpAuthToBpmnProcessExistenceValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.bpmn.BpmnFileValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.bpmn.BpmnFileGroupUniqueProcessIdValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.dmn.DmnFileValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.excerpt.ExcerptTemplateUniqueNameValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.file.EmptyFileValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.file.FileExistenceValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.file.FileExtensionValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.file.FileGroupValidatorLoggingDecorator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.file.FileValidatorLoggingDecorator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.file.*;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.json.JsonSchemaFileValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.mainliquibase.MainLiquibaseRulesValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.datasettings.DatafactorySettingsYamlRulesValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.bpmn.BpTrembitaToBpmnProcessExistenceValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.typed.BpAuthProcessUniquenessValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.typed.BpTrembitaProcessUniquenessValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.var.GlobalVarsFileValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.ResourceLoader;

public class RegulationValidatorFactory {

  private static final String BP_AUTH_JSON_SCHEMA = "classpath:schema/bp-auth-schema.json";
  private static final String BP_TREMBITA_JSON_SCHEMA = "classpath:schema/bp-trembita-schema.json";
  private static final String BP_TREMBITA_CONFIG_JSON_SCHEMA = "classpath:schema/bp-trembita-config-schema.json";
  private static final String ROLES_JSON_SCHEMA = "classpath:schema/roles-schema.json";
  private static final String GLOBAL_VARS_JSON_SCHEMA = "classpath:schema/global-vars-schema.json";
  private static final String FORMS_JSON_SCHEMA = "classpath:schema/forms-schema.json";
  private static final String REGISTRY_SETTINGS_JSON_SCHEMA = "classpath:schema/registry-settings-schema.json";

  private final Map<RegulationFileType, RegulationValidator<File>> regulationTypeValidators;
  private final Map<RegulationFileType, RegulationValidator<Collection<File>>> groupRegulationTypeValidators;
  private final Map<RegulationFileType, RegulationValidator<RegulationFiles>> globalRegulationTypeValidators;

  public RegulationValidatorFactory(ResourceLoader resourceLoader, ObjectMapper yamlObjectMapper, ObjectMapper jsonObjectMapper,
                                      RuleBook<Set<ValidationError>> settingsYamlRuleBook, RuleBook<Set<ValidationError>> mainLiquibaseRuleBook) {
    this.regulationTypeValidators = regulationTypeValidators(resourceLoader, yamlObjectMapper, jsonObjectMapper,
                settingsYamlRuleBook, mainLiquibaseRuleBook);
    this.groupRegulationTypeValidators = regulationTypeGroupValidators();
    this.globalRegulationTypeValidators = globalRegulationTypeValidators(yamlObjectMapper);
  }

  public RegulationValidator<RegulationFiles> newRegulationFilesValidator() {
    return new RegulationFilesValidator(regulationTypeValidators, groupRegulationTypeValidators, globalRegulationTypeValidators);
  }

  private Map<RegulationFileType, RegulationValidator<File>> regulationTypeValidators(ResourceLoader resourceLoader, ObjectMapper yamlObjectMapper,
      ObjectMapper jsonObjectMapper, RuleBook<Set<ValidationError>> settingsYamlRuleBook,
      RuleBook<Set<ValidationError>> mainLiquibaseRuleBook) {
    Map<RegulationFileType, RegulationValidator<File>> validators = new EnumMap<>(RegulationFileType.class);
    validators.put(RegulationFileType.BP_AUTH, newBpAuthFileValidator(resourceLoader, yamlObjectMapper));
    validators.put(RegulationFileType.BP_TREMBITA, newBpTrembitaFileValidator(resourceLoader, yamlObjectMapper));
    validators.put(RegulationFileType.BP_TREMBITA_CONFIG, newBpTrembitaConfigFileValidator(resourceLoader, yamlObjectMapper));
    validators.put(RegulationFileType.ROLES, newRolesFileValidator(resourceLoader, yamlObjectMapper));
    validators.put(RegulationFileType.GLOBAL_VARS, newGlobalVarsFileValidator(resourceLoader, yamlObjectMapper));
    validators.put(RegulationFileType.FORMS, newFormsFileValidator(resourceLoader, jsonObjectMapper));
    validators.put(RegulationFileType.BPMN, newBpmnFileValidator());
    validators.put(RegulationFileType.DMN, newDmnFileValidator());
    validators.put(RegulationFileType.DATAFACTORY_SETTINGS, newDataFactorySettingsFileValidator(yamlObjectMapper, settingsYamlRuleBook));
    validators.put(RegulationFileType.REGISTRY_SETTINGS, newRegistrySettingsFileValidator(resourceLoader, yamlObjectMapper));
    validators.put(RegulationFileType.LIQUIBASE, newMainLiquibaseFileValidator(mainLiquibaseRuleBook));
    return validators;
  }

  private Map<RegulationFileType, RegulationValidator<Collection<File>>> regulationTypeGroupValidators() {
    return Map.of(
        RegulationFileType.BPMN, newBpmnFileGroupValidator(),
        RegulationFileType.EXCERPTS, newExcerptGroupValidator()
    );
  }

  private Map<RegulationFileType, RegulationValidator<RegulationFiles>> globalRegulationTypeValidators(
          ObjectMapper yamlObjectMapper) {
    return Map.of(
            RegulationFileType.BP_AUTH_TO_BPMN, newBpAuthToProcessDefinitionIdsValidator(yamlObjectMapper),
            RegulationFileType.BP_TREMBITA_TO_BPMN, newBpTrembitaToBpmnProcessDefinitionIdsValidator(yamlObjectMapper)

    );
  }

  private RegulationValidator<RegulationFiles> newBpAuthToProcessDefinitionIdsValidator(ObjectMapper yamlObjectMapper) {
    return decorateGlobalValidator(GlobalCompositeRegulationFilesValidator.builder()
            .validator(new BpAuthToBpmnProcessExistenceValidator(yamlObjectMapper))
            .build());
  }

  private RegulationValidator<RegulationFiles> newBpTrembitaToBpmnProcessDefinitionIdsValidator(ObjectMapper yamlObjectMapper) {
    return decorateGlobalValidator(GlobalCompositeRegulationFilesValidator.builder()
            .validator(new BpTrembitaToBpmnProcessExistenceValidator(yamlObjectMapper))
            .build());
  }

  private RegulationValidator<Collection<File>> newBpmnFileGroupValidator() {
    return decorateGroupValidator(
        CompositeFileGroupValidator.builder()
            .validator(new BpmnFileGroupUniqueProcessIdValidator())
            .build()
    );
  }

  private RegulationValidator<Collection<File>> newExcerptGroupValidator() {
    return decorateGroupValidator(
        CompositeFileGroupValidator.builder()
            .validator(new ExcerptTemplateUniqueNameValidator())
            .build()
    );
  }

  private RegulationValidator<File> newBpAuthFileValidator(ResourceLoader resourceLoader, ObjectMapper yamlObjectMapper) {
    return decorate(
        CompositeFileValidator.builder()
            .validator(new FileExistenceValidator())
            .validator(new FileExtensionValidator())
            .validator(new EmptyFileValidator())
            .validator(new JsonSchemaFileValidator(BP_AUTH_JSON_SCHEMA, resourceLoader, yamlObjectMapper))
            .validator(
                TypedConfigurationValidator.<BpAuthConfiguration>builder()
                    .configurationClass(BpAuthConfiguration.class)
                    .configurationLoader(new RegulationConfigurationLoader(yamlObjectMapper))
                    .validator(new BpAuthProcessUniquenessValidator())
                    .build())
            .build()
    );
  }

  private RegulationValidator<File> newBpTrembitaFileValidator(ResourceLoader resourceLoader, ObjectMapper yamlObjectMapper) {
    return decorate(
        CompositeFileValidator.builder()
            .validator(new FileExistenceValidator())
            .validator(new FileExtensionValidator())
            .validator(new EmptyFileValidator())
            .validator(new JsonSchemaFileValidator(BP_TREMBITA_JSON_SCHEMA, resourceLoader, yamlObjectMapper))
            .validator(
                TypedConfigurationValidator.<BpTrembitaConfiguration>builder()
                    .configurationClass(BpTrembitaConfiguration.class)
                    .configurationLoader(new RegulationConfigurationLoader(yamlObjectMapper))
                    .validator(new BpTrembitaProcessUniquenessValidator())
                    .build())
            .build()
    );
  }

  private RegulationValidator<File> newBpTrembitaConfigFileValidator(ResourceLoader resourceLoader, ObjectMapper yamlObjectMapper) {
    return decorate(
        CompositeFileValidator.builder()
            .validator(new FileExistenceValidator())
            .validator(new FileExtensionValidator())
            .validator(new EmptyFileValidator())
            .validator(new JsonSchemaFileValidator(BP_TREMBITA_CONFIG_JSON_SCHEMA, resourceLoader, yamlObjectMapper))
            .build()
    );
  }

  private RegulationValidator<File> newRolesFileValidator(ResourceLoader resourceLoader, ObjectMapper yamlObjectMapper) {
    return decorate(
        CompositeFileValidator.builder()
            .validator(new FileExistenceValidator())
            .validator(new FileExtensionValidator())
            .validator(new EmptyFileValidator())
            .validator(new JsonSchemaFileValidator(ROLES_JSON_SCHEMA, resourceLoader, yamlObjectMapper))
            .build()
    );
  }

  private RegulationValidator<File> newGlobalVarsFileValidator(ResourceLoader resourceLoader, ObjectMapper yamlObjectMapper) {
    return decorate(
        CompositeFileValidator.builder()
            .validator(new FileExistenceValidator())
            .validator(new FileExtensionValidator())
            .validator(new EmptyFileValidator())
            .validator(new GlobalVarsFileValidator(GLOBAL_VARS_JSON_SCHEMA, resourceLoader, yamlObjectMapper))
            .build()
    );
  }

  private RegulationValidator<File> newFormsFileValidator(ResourceLoader resourceLoader, ObjectMapper jsonObjectMapper) {
    return decorate(
        CompositeFileValidator.builder()
            .validator(new FileExistenceValidator())
            .validator(new FileExtensionValidator())
            .validator(new EmptyFileValidator())
            .validator(new FormsFileValidator(FORMS_JSON_SCHEMA, resourceLoader, jsonObjectMapper))
            .build()
    );
  }

  private RegulationValidator<File> newBpmnFileValidator() {
    return decorate(
        CompositeFileValidator.builder()
            .validator(new FileExistenceValidator())
            .validator(new FileExtensionValidator())
            .validator(new BpmnFileValidator())
            .build()
    );
  }

  private RegulationValidator<File> newDmnFileValidator() {
    return decorate(
        CompositeFileValidator.builder()
            .validator(new FileExistenceValidator())
            .validator(new FileExtensionValidator())
            .validator(new DmnFileValidator())
            .build()
    );
  }

  private RegulationValidator<File> decorate(RegulationValidator<File> validator) {
    return FileValidatorLoggingDecorator.wrap(validator);
  }

  private RegulationValidator<Collection<File>> decorateGroupValidator(
      RegulationValidator<Collection<File>> validator) {
    return FileGroupValidatorLoggingDecorator.wrap(validator);
  }

  private RegulationValidator<RegulationFiles> decorateGlobalValidator(
          RegulationValidator<RegulationFiles> validator) {
    return GlobalFileValidatorLoggingDecorator.wrap(validator);
  }

  private RegulationValidator<File> newDataFactorySettingsFileValidator(ObjectMapper jsonObjectMapper,
                                                                        RuleBook<Set<ValidationError>> settingsYamlRuleBook) {
    return decorate(
            CompositeFileValidator.builder()
                    .validator(new FileExistenceValidator())
                    .validator(new FileExtensionValidator())
                    .validator(new DatafactorySettingsYamlRulesValidator(jsonObjectMapper, settingsYamlRuleBook))
                    .build()
    );
  }

  private RegulationValidator<File> newRegistrySettingsFileValidator(
      ResourceLoader resourceLoader, ObjectMapper yamlObjectMapper) {
    return decorate(
        CompositeFileValidator.builder()
            .validator(new FileExistenceValidator())
            .validator(new FileExtensionValidator())
            .validator(new EmptyFileValidator())
            .validator(
                new JsonSchemaFileValidator(
                    REGISTRY_SETTINGS_JSON_SCHEMA, resourceLoader, yamlObjectMapper))
            .build());
  }

  private RegulationValidator<File> newMainLiquibaseFileValidator(RuleBook<Set<ValidationError>> mainLiquibaseRuleBook) {
    return decorate(
            CompositeFileValidator.builder()
                    .validator(new FileExistenceValidator())
                    .validator(new FileExtensionValidator())
                    .validator(new MainLiquibaseRulesValidator(mainLiquibaseRuleBook))
                    .build()
    );
  }
}
