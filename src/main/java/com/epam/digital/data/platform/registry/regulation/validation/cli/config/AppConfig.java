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

package com.epam.digital.data.platform.registry.regulation.validation.cli.config;

import com.deliveredtechnologies.rulebook.model.RuleBook;
import com.deliveredtechnologies.rulebook.spring.SpringAwareRuleBookRunner;
import com.epam.digital.data.platform.registry.regulation.validation.cli.RegulationValidationCommandLineRunner;
import com.epam.digital.data.platform.registry.regulation.validation.cli.support.CommandLineArgsParser;
import com.epam.digital.data.platform.registry.regulation.validation.cli.support.CommandLineOptionsConverter;
import com.epam.digital.data.platform.registry.regulation.validation.cli.support.SystemExit;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.RegulationValidatorFactory;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationError;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.util.Set;

@Configuration
public class AppConfig {

  @Bean
  @Autowired
  public CommandLineRunner commandLineRunner(RegulationValidatorFactory regulationValidatorFactory, SystemExit systemExit) {
    return new RegulationValidationCommandLineRunner(regulationValidatorFactory, new CommandLineArgsParser(), new CommandLineOptionsConverter(), systemExit);
  }

  @Bean
  @Autowired
  public RegulationValidatorFactory registryRegulationValidatorFactory(ResourceLoader resourceLoader) {
    return new RegulationValidatorFactory(resourceLoader, yamlObjectMapper(), jsonObjectMapper(),
            settingsYamlRuleBook(), mainLiquibaseRuleBook());
  }

  @Bean
  @Autowired
  public SystemExit systemExit(ApplicationContext context) {
    return new SystemExit(context);
  }

  private YAMLMapper yamlObjectMapper() {
    return new YAMLMapper();
  }

  private JsonMapper jsonObjectMapper() {
    return new JsonMapper();
  }

  @Bean
  public RuleBook<Set<ValidationError>> settingsYamlRuleBook() {
    return getRuleBookRunner(
            "com.epam.digital.data.platform.registry.regulation.validation.cli.validator.settings.rules");
  }

  @Bean
  public RuleBook<Set<ValidationError>> mainLiquibaseRuleBook() {
    return getRuleBookRunner(
            "com.epam.digital.data.platform.registry.regulation.validation.cli.validator.mainliquibase.rules");
  }

  @SuppressWarnings("unchecked")
  private RuleBook<Set<ValidationError>> getRuleBookRunner(String rulePackage)  {
    var springAwareRuleBookRunner = new SpringAwareRuleBookRunner(rulePackage);

    springAwareRuleBookRunner.setDefaultResult(Sets.newHashSet());
    return springAwareRuleBookRunner;
  }
}
