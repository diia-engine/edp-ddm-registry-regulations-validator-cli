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

import static org.camunda.bpm.model.bpmn.impl.BpmnModelConstants.CAMUNDA_NS;

import com.epam.digital.data.platform.registry.regulation.validation.cli.model.ElementTemplate;
import com.epam.digital.data.platform.registry.regulation.validation.cli.model.ElementTemplate.Property;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.RegulationValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationContext;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationError;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Activity;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaIn;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaInputOutput;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaInputParameter;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaOut;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaOutputParameter;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.CollectionUtils;

@Slf4j
public class BpmnFileInputsValidator implements RegulationValidator<File> {

  private static final Map<String, BiFunction<Activity, Property, Set<String>>> GET_VALUES_FOR_VALIDATION_FUNCTIONS = Map.of(
      "property", BpmnFileInputsValidator::getAttributeValuesFromActivity,
      "camunda:property", BpmnFileInputsValidator::getPropertyValueFromActivity,
      "camunda:in", BpmnFileInputsValidator::getInValueFromActivity,
      "camunda:out", BpmnFileInputsValidator::getOutValueFromActivity,
      "camunda:inputParameter", BpmnFileInputsValidator::getInputParameterValueFromActivity,
      "camunda:outputParameter", BpmnFileInputsValidator::getOutputParameterValueFromActivity
  );

  private final Map<String, ElementTemplate> elementTemplates;

  public BpmnFileInputsValidator(String elementTemplatePath) {
    var jsonFile = new File(elementTemplatePath);

    var objectMapper = new Jackson2ObjectMapperBuilder()
        .failOnUnknownProperties(false)
        .build();

    try {
      elementTemplates = objectMapper.readValue(jsonFile, new ElementTemplateListTypeReference())
          .stream()
          .collect(Collectors.toMap(ElementTemplate::getId, Function.identity()));
    } catch (IOException e) {
      throw new IllegalStateException("During reading elementTemplates file occurred error.", e);
    }
  }

  @Override
  public Set<ValidationError> validate(File regulation, ValidationContext context) {
    return validateElementTemplateParameters(loadProcessModel(regulation), regulation, context);
  }

  private Set<ValidationError> validateElementTemplateParameters(BpmnModelInstance bpmnModel,
      File regulationFile, ValidationContext validationContext) {
    var validationErrors = new HashSet<ValidationError>();
    var elements = bpmnModel.getModelElementsByType(Activity.class)
        .stream()
        .filter(element -> Objects.nonNull(
            element.getAttributeValueNs(CAMUNDA_NS, "modelerTemplate")))
        .collect(Collectors.toList());

    for (var element : elements) {
      var modelerTemplate = element.getAttributeValueNs(CAMUNDA_NS, "modelerTemplate");
      var elementTemplate = elementTemplates.get(modelerTemplate);
      if (Objects.isNull(elementTemplate)) {
        log.warn("No element template with id {} found", modelerTemplate);
        continue;
      }
      validationErrors.addAll(
          validateElementAgainstElementTemplate(element, elementTemplate, regulationFile,
              validationContext));
    }

    return validationErrors;
  }

  private Set<ValidationError> validateElementAgainstElementTemplate(Activity activity,
      ElementTemplate elementTemplate, File regulationFile, ValidationContext validationContext) {
    var properties = elementTemplate.getProperties();

    return properties.stream()
        .map(property -> validatePropertyInElement(activity, property, regulationFile,
            validationContext))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private ValidationError validatePropertyInElement(Activity activity,
      ElementTemplate.Property property, File regulationFile, ValidationContext validationContext) {
    var getValuesFunction = GET_VALUES_FOR_VALIDATION_FUNCTIONS.get(
        property.getBinding().getType());
    var propertyValue = getValuesFunction.apply(activity, property);

    if (propertyValue.size() > 1) {
      return ValidationError.of(validationContext.getRegulationFileType(), regulationFile,
          String.format("In task %s in process %s there are several values for input parameter %s",
              activity.getId(), regulationFile.getName(), property.getBinding()));
    }

    if (property.getConstraints().isNotEmpty() && StringUtils.isBlank(
        CollectionUtils.firstElement(propertyValue))) {
      return ValidationError.of(validationContext.getRegulationFileType(), regulationFile,
          String.format("In task %s in process %s input parameter %s is empty",
              activity.getId(), regulationFile.getName(), property.getBinding()));
    }
    return null;
  }

  private BpmnModelInstance loadProcessModel(File regulationFile) {
    return Bpmn.readModelFromFile(regulationFile);
  }

  @VisibleForTesting
  static Set<String> getAttributeValuesFromActivity(Activity activity, Property property) {
    var attributeName = property.getBinding().getName();
    var namespaceAndName = attributeName.split(":");

    var attributeValue = namespaceAndName.length == 1 ?
        activity.getAttributeValue(attributeName) :
        activity.getAttributeValueNs(CAMUNDA_NS, namespaceAndName[1]);

    return Objects.isNull(attributeValue) ? Set.of() : Set.of(attributeValue);
  }

  @VisibleForTesting
  static Set<String> getPropertyValueFromActivity(Activity activity, Property property) {
    var propertyName = property.getBinding().getName();
    var extensionElements = activity.getExtensionElements();
    if (Objects.isNull(extensionElements)) {
      return Set.of();
    }
    return extensionElements.getElementsQuery()
        .filterByType(CamundaProperties.class)
        .list()
        .stream()
        .flatMap(camundaProperties -> camundaProperties.getCamundaProperties().stream())
        .filter(prop -> prop.getCamundaName().equals(propertyName))
        .map(CamundaProperty::getCamundaValue)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  @VisibleForTesting
  static Set<String> getInValueFromActivity(Activity activity, Property property) {
    var propertyName = property.getBinding().getName();
    var extensionElements = activity.getExtensionElements();
    if (Objects.isNull(extensionElements)) {
      return Set.of();
    }
    return extensionElements.getElementsQuery()
        .filterByType(CamundaIn.class)
        .list()
        .stream()
        .filter(in -> in.getAttributeValue("target").equals(propertyName))
        .map(in -> in.getAttributeValue("sourceExpression"))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  @VisibleForTesting
  static Set<String> getOutValueFromActivity(Activity activity, Property property) {
    var propertyName = property.getBinding().getSource();
    var extensionElements = activity.getExtensionElements();
    if (Objects.isNull(extensionElements)) {
      return Set.of();
    }
    return extensionElements.getElementsQuery()
        .filterByType(CamundaOut.class)
        .list()
        .stream()
        .filter(in -> in.getAttributeValue("source").equals(propertyName))
        .map(in -> in.getAttributeValue("target"))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  @VisibleForTesting
  static Set<String> getInputParameterValueFromActivity(Activity activity, Property property) {
    var propertyName = property.getBinding().getName();
    var extensionElements = activity.getExtensionElements();
    if (Objects.isNull(extensionElements)) {
      return Set.of();
    }
    return extensionElements.getElementsQuery()
        .filterByType(CamundaInputOutput.class)
        .list()
        .stream()
        .flatMap(cio -> cio.getCamundaInputParameters().stream())
        .filter(cip -> cip.getCamundaName().equals(propertyName))
        .map(CamundaInputParameter::getTextContent)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  @VisibleForTesting
  static Set<String> getOutputParameterValueFromActivity(Activity activity, Property property) {
    var propertyName = property.getBinding().getSource();
    var extensionElements = activity.getExtensionElements();
    if (Objects.isNull(extensionElements)) {
      return Set.of();
    }
    return extensionElements.getElementsQuery()
        .filterByType(CamundaInputOutput.class)
        .list()
        .stream()
        .flatMap(cio -> cio.getCamundaOutputParameters().stream())
        .filter(cop -> cop.getTextContent().equals(propertyName))
        .map(CamundaOutputParameter::getCamundaName)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private static class ElementTemplateListTypeReference extends
      TypeReference<List<ElementTemplate>> {

  }
}
