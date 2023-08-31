/*
 * Copyright 2023 EPAM Systems.
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelException;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.xml.ModelParseException;
import org.camunda.bpm.model.xml.ModelValidationException;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

@Slf4j
public class BpmnFileValidator implements RegulationValidator<File> {

  private static final String MISSING_PROCESS_NAME_ERROR_MSG_FORMAT = "Process definition '%s' must have 'name' attribute defined";
  private static final String UNABLE_VALIDATE_JUEL_FUNCTION_MSG_FORMAT = "Unable to validate %s() juel function due to dynamic value: %s";
  private static final Map<String, List<Class<?>>> juelFunctions = Map.of(
      "completer", List.of(Task.class),
      "message_payload", List.of(ModelElementInstance.class),
      "sign_submission", List.of(Task.class, StartEvent.class),
      "submission", List.of(Task.class, StartEvent.class));

  @Override
  public Set<ValidationError> validate(File regulationFile, ValidationContext validationContext) {
    try {
      var bpmnModel = loadProcessModel(regulationFile);
      var xmlContent = FileUtils.readFileToString(regulationFile);
      return validateProcessModel(bpmnModel, regulationFile, validationContext, xmlContent);
    } catch (BpmnModelException | ModelParseException | IOException ex) {
      return Collections.singleton(
          ValidationError.of(validationContext.getRegulationFileType(), regulationFile,
              createErrorMsg(ex), ex)
      );
    }
  }

  private String createErrorMsg(Throwable ex) {
    return new StringJoiner(", ")
        .add("BPMN file parsing failure")
        .add(Objects.nonNull(ex.getCause()) ? ex.getCause().getMessage() : ex.getMessage())
        .toString();
  }

  private Set<ValidationError> validateProcessModel(BpmnModelInstance bpmnModel,
      File regulationFile, ValidationContext validationContext, String xmlContent) {
    var errors = new LinkedHashSet<ValidationError>();
    errors.addAll(validateSchema(bpmnModel, regulationFile, validationContext));
    errors.addAll(validateProcess(bpmnModel, regulationFile, validationContext));
    juelFunctions.forEach(
        (key, value) -> validateJuelFunction(key, value, bpmnModel, regulationFile,
            validationContext, xmlContent, errors));

    return errors;
  }

  private Set<ValidationError> validateSchema(BpmnModelInstance bpmnModel, File regulationFile,
      ValidationContext validationContext) {
    try {
      Bpmn.validateModel(bpmnModel);
      return Collections.emptySet();
    } catch (ModelValidationException ex) {
      return Collections.singleton(
          ValidationError.of(validationContext.getRegulationFileType(), regulationFile,
              "BPMN file validation against the schema failure", ex)
      );
    }
  }

  private Set<ValidationError> validateProcess(BpmnModelInstance bpmnModel, File regulationFile,
      ValidationContext validationContext) {
    var processes = bpmnModel.getModelElementsByType(Process.class);
    return processes.stream()
        .filter(process -> StringUtils.isBlank(process.getName()))
        .map(process -> toValidationError(process, regulationFile, validationContext))
        .collect(Collectors.toSet());
  }

  private void validateJuelFunction(String functionName,
      List<Class<?>> modelElementClass, BpmnModelInstance bpmnModel,
      File regulationFile, ValidationContext validationContext, String xmlContent,
      Set<ValidationError> errors) {
    var values = getJuelFunctionsValues(functionName, xmlContent);

    for (var value : values) {
      var valueWithoutQuotes = extractValue(value);
      if (Objects.isNull(valueWithoutQuotes)) {
        log.warn(
            String.format(UNABLE_VALIDATE_JUEL_FUNCTION_MSG_FORMAT, functionName, value));
        continue;
      }
      var element = bpmnModel.getModelElementById(valueWithoutQuotes);
      if (Objects.isNull(element) || modelElementClass.stream()
          .noneMatch(c -> c.isAssignableFrom(element.getClass()))) {
        errors.add(ValidationError.of(validationContext.getRegulationFileType(), regulationFile,
            String.format("Element id %s not found for %s() juel function", value, functionName)));
      }
    }
  }

  private List<String> getJuelFunctionsValues(String functionName, String xmlContent) {
    String pattern = "(?<!\\w)" + functionName + "\\((.*?)\\)";
    Pattern regexPattern = Pattern.compile(pattern);
    Matcher matcher = regexPattern.matcher(xmlContent);
    List<String> values = new ArrayList<>();
    while (matcher.find()) {
      values.add(matcher.group(1));
    }
    return values;
  }

  private String extractValue(String value) {
    String pattern = "^(\"[^\"']*\"|'[^\"']*')$";
    Pattern regexPattern = Pattern.compile(pattern);
    Matcher matcher = regexPattern.matcher(value);
    if (matcher.matches()) {
      return value.substring(1, value.length() - 1);
    }
    return null;
  }

  private ValidationError toValidationError(Process process, File regulationFile,
      ValidationContext validationContext) {
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
