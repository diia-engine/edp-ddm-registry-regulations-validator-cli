/*
 * Copyright 2021 EPAM Systems.
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

package com.epam.digital.data.platform.registry.regulation.validation.cli.validator.bpmn;

import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.RegulationValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationContext;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationError;
import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelException;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelParseException;
import org.camunda.bpm.model.xml.ModelValidationException;

public class BpmnFileValidator implements RegulationValidator<File> {

  private static final String MISSING_PROCESS_NAME_ERROR_MSG_FORMAT = "Process definition '%s' must have 'name' attribute defined";

  @Override
  public Set<ValidationError> validate(File regulationFile, ValidationContext validationContext) {
    try {
      var bpmnModel = loadProcessModel(regulationFile);
      return validateProcessModel(bpmnModel, regulationFile, validationContext);
    } catch (BpmnModelException | ModelParseException ex) {
      return Collections.singleton(
          ValidationError.of(validationContext.getRegulationFileType(), regulationFile, createErrorMsg(ex), ex)
      );
    }
  }

  private String createErrorMsg(Throwable ex) {
    return new StringJoiner(", ")
        .add("BPMN file parsing failure")
        .add(Objects.nonNull(ex.getCause()) ? ex.getCause().getMessage() : ex.getMessage())
        .toString();
  }

  private Set<ValidationError> validateProcessModel(BpmnModelInstance bpmnModel, File regulationFile, ValidationContext validationContext) {
    var errors = new LinkedHashSet<ValidationError>();
    errors.addAll(validateSchema(bpmnModel, regulationFile, validationContext));
    errors.addAll(validateProcess(bpmnModel, regulationFile, validationContext));
    return errors;
  }

  private Set<ValidationError> validateSchema(BpmnModelInstance bpmnModel, File regulationFile, ValidationContext validationContext) {
    try {
      Bpmn.validateModel(bpmnModel);
      return Collections.emptySet();
    } catch (ModelValidationException ex) {
      return Collections.singleton(
          ValidationError.of(validationContext.getRegulationFileType(), regulationFile, "BPMN file validation against the schema failure", ex)
      );
    }
  }

  private Set<ValidationError> validateProcess(BpmnModelInstance bpmnModel, File regulationFile, ValidationContext validationContext) {
    var processes = bpmnModel.getModelElementsByType(Process.class);
    return processes.stream()
        .filter(process -> StringUtils.isBlank(process.getName()))
        .map(process -> toValidationError(process, regulationFile, validationContext))
        .collect(Collectors.toSet());
  }

  private ValidationError toValidationError(Process process, File regulationFile, ValidationContext validationContext) {
    return ValidationError.builder()
        .regulationFileType(validationContext.getRegulationFileType())
        .regulationFile(regulationFile)
        .errorMessage(String.format(MISSING_PROCESS_NAME_ERROR_MSG_FORMAT, process.getId()))
        .build();
  }

  private BpmnModelInstance loadProcessModel(File regulationFile) {
    return Bpmn.readModelFromFile(regulationFile);
  }
}
