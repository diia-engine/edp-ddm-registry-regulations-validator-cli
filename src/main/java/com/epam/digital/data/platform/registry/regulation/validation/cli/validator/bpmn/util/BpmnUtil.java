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

package com.epam.digital.data.platform.registry.regulation.validation.cli.validator.bpmn.util;

import com.epam.digital.data.platform.registry.regulation.validation.cli.exception.FileProcessingException;
import com.epam.digital.data.platform.registry.regulation.validation.cli.model.BpRoleConfiguration;
import com.epam.digital.data.platform.registry.regulation.validation.cli.model.RegulationFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.Process;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class BpmnUtil {

  private static final ObjectMapper yamlMapper = new YAMLMapper();

  public static Set<String> getBpmnFilesProcessDefinitionsId(RegulationFiles regulationFiles) {
    Set<String> bpmnProcessDefinitionsId = new HashSet<>();
    for (File file : regulationFiles.getBpmnFiles()) {
      if (file.exists()) {
        Collection<Process> modelElementsByType = Bpmn.readModelFromFile(file).getModelElementsByType(Process.class);
        bpmnProcessDefinitionsId.addAll(modelElementsByType.stream().map(BaseElement::getId).collect(Collectors.toSet()));
      }
    }
    return bpmnProcessDefinitionsId;
  }

  public static Set<String> getRoles(RegulationFiles regulationFiles) {
    Set<String> roles = new HashSet<>();

    for (File file : regulationFiles.getRolesFiles()) {
      try {
        if (file.exists()) {
          roles.addAll(yamlMapper.readValue(file, BpRoleConfiguration.class)
              .getRoles()
              .stream()
              .map(BpRoleConfiguration.Role::getName)
              .collect(Collectors.toSet()));
        }
      } catch (IOException e) {
        throw new FileProcessingException("File processing failure", e);
      }
    }
    return roles;
  }
}
