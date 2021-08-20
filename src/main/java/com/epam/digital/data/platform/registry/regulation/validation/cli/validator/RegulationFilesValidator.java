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

import com.epam.digital.data.platform.registry.regulation.validation.cli.model.RegulationFileType;
import com.epam.digital.data.platform.registry.regulation.validation.cli.model.RegulationFiles;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class RegulationFilesValidator implements RegulationValidator<RegulationFiles> {

  private final Map<RegulationFileType, RegulationValidator<File>> regulationTypeValidators;
  private final Map<RegulationFileType, RegulationValidator<Collection<File>>> groupRegulationTypeValidators;
  private final Map<RegulationFileType, RegulationValidator<RegulationFiles>> globalRegulationTypeValidators;

  public RegulationFilesValidator(
          Map<RegulationFileType, RegulationValidator<File>> regulationTypeValidators,
          Map<RegulationFileType, RegulationValidator<Collection<File>>> groupRegulationTypeValidators,
          Map<RegulationFileType, RegulationValidator<RegulationFiles>> globalRegulationTypeValidators) {
    this.regulationTypeValidators = regulationTypeValidators;
    this.groupRegulationTypeValidators = groupRegulationTypeValidators;
    this.globalRegulationTypeValidators = globalRegulationTypeValidators;
  }

  @Override
  public Set<ValidationError> validate(RegulationFiles regulationFiles, ValidationContext context) {
    var errors = new LinkedHashSet<ValidationError>();

    regulationFiles.getBpAuthFiles().forEach(file -> errors.addAll(validate(file, RegulationFileType.BP_AUTH)));

    regulationFiles.getBpTrembitaFiles().forEach(file -> errors.addAll(validate(file, RegulationFileType.BP_TREMBITA)));

    regulationFiles.getBpTrembitaConfig().forEach(file -> errors.addAll(validate(file, RegulationFileType.BP_TREMBITA_CONFIG)));

    regulationFiles.getGlobalVarsFiles().forEach(file -> errors.addAll(validate(file, RegulationFileType.GLOBAL_VARS)));

    regulationFiles.getRolesFiles().forEach(file -> errors.addAll(validate(file, RegulationFileType.ROLES)));

    errors.addAll(validateBpmnFiles(regulationFiles.getBpmnFiles()));

    regulationFiles.getDmnFiles().forEach(file -> errors.addAll(validate(file, RegulationFileType.DMN)));

    regulationFiles.getFormFiles().forEach(file -> errors.addAll(validate(file, RegulationFileType.FORMS)));

    regulationFiles.getSettingsFiles().forEach(file -> errors.addAll(validate(file, RegulationFileType.SETTINGS)));

    regulationFiles.getLiquibaseFiles().forEach(file -> errors.addAll(validate(file, RegulationFileType.LIQUIBASE)));
    
    errors.addAll(validateExcerptFiles(regulationFiles.getExcerptFiles()));

    errors.addAll(validateGlobalFiles(regulationFiles, RegulationFileType.BP_AUTH_TO_BPMN));

    errors.addAll(validateGlobalFiles(regulationFiles, RegulationFileType.BP_TREMBITA_TO_BPMN));

    return errors;
  }

  private Collection<ValidationError> validateGlobalFiles(RegulationFiles regulationFiles, RegulationFileType regulationFileType) {
    return globalRegulationTypeValidators.get(regulationFileType).validate(regulationFiles, ValidationContext.of(regulationFileType));
  }

  private Set<ValidationError> validate(File file, RegulationFileType regulationFileType) {
    var validator = this.regulationTypeValidators.get(regulationFileType);
    return validator.validate(file, ValidationContext.of(regulationFileType));
  }

  private Set<ValidationError> validateBpmnFiles(Collection<File> bpmnFiles) {
    Set<ValidationError> errors = Sets.newHashSet();
    bpmnFiles.forEach(file -> errors.addAll(validate(file, RegulationFileType.BPMN)));

    if (errors.isEmpty()) {
      var groupValidator = groupRegulationTypeValidators.get(RegulationFileType.BPMN);
      errors.addAll(groupValidator.validate(bpmnFiles, ValidationContext.of(RegulationFileType.BPMN)));
    }
    return errors;
  }

  private Set<ValidationError> validateExcerptFiles(Collection<File> excerptFolders) {
    Set<ValidationError> errors = Sets.newHashSet();
    var groupValidator = groupRegulationTypeValidators.get(RegulationFileType.EXCERPTS);
    errors.addAll(groupValidator.validate(excerptFolders, ValidationContext.of(RegulationFileType.EXCERPTS)));
    return errors;
  }
}
