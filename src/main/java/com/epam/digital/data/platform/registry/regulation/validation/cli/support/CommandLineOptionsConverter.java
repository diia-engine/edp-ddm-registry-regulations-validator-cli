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

package com.epam.digital.data.platform.registry.regulation.validation.cli.support;

import com.epam.digital.data.platform.registry.regulation.validation.cli.model.RegulationFiles;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

public class CommandLineOptionsConverter {

  public RegulationFiles convert(Options options) {
    return RegulationFiles.builder()
        .globalVarsFiles(getFilesIfAny(CommandLineArg.GLOBAL_VARS, options))
        .bpAuthFiles(getFilesIfAny(CommandLineArg.BP_AUTH, options))
        .bpTrembitaFiles(getFilesIfAny(CommandLineArg.BP_TREMBITA, options))
        .bpTrembitaConfig(getFilesIfAny(CommandLineArg.BP_TREMBITA_CONFIG, options))
        .rolesFiles(getFilesIfAny(CommandLineArg.ROLES, options))
        .bpmnFiles(getFilesIfAny(CommandLineArg.BPMN, options))
        .dmnFiles(getFilesIfAny(CommandLineArg.DMN, options))
        .formFiles(getFilesIfAny(CommandLineArg.FORMS, options))
        .settingsFiles(getFilesIfAny(CommandLineArg.SETTINGS, options))
        .liquibaseFiles(getFilesIfAny(CommandLineArg.LIQUIBASE, options))
        .excerptFiles(getFilesIfAny(CommandLineArg.EXCERPTS, options))
        .build();
  }

  private Collection<File> getFilesIfAny(CommandLineArg regulationTypeArgOption, Options options) {
    if (options.hasOption(regulationTypeArgOption.getArgOptionName())) {
      var option = options.getOption(regulationTypeArgOption.getArgOptionName());
      var optionValues = option.getValues();
      return Stream.of(optionValues).filter(StringUtils::isNotBlank).map(File::new).collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }
}
