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

package com.epam.digital.data.platform.registry.regulation.validation.cli.validator.bpgrouping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.epam.digital.data.platform.registry.regulation.validation.cli.model.RegulationFileType;
import com.epam.digital.data.platform.registry.regulation.validation.cli.model.RegulationFiles;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationContext;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BpGroupingProcessDefinitionIdValidatorTest {

  private BpGroupingProcessDefinitionIdValidator validator;

  @BeforeEach
  public void setUp() {
    this.validator = new BpGroupingProcessDefinitionIdValidator(new YAMLMapper());
  }

  @Test
  void shouldPassBpGroupingProcessDefinitionIdValidation() {
    var regulationFiles = RegulationFiles.builder()
        .bpmnFiles(List.of(
            getFileFromClasspath("registry-regulation/correct/bp-grouping/bpmn/process_for_group_1.bpmn"),
            getFileFromClasspath("registry-regulation/correct/bp-grouping/bpmn/process_for_group_2.bpmn")))
        .bpGroupingFiles(Collections.singleton(
            getFileFromClasspath("registry-regulation/correct/bp-grouping/bp-grouping.yml")))
        .build();

    var errors = validator.validate(regulationFiles,
        ValidationContext.of(RegulationFileType.BP_GROUPING_TO_BPMN));

    assertThat(errors, is(empty()));
  }

  @Test
  void shouldFailBpGroupingProcessDefinitionIdDuplicatesValidation() {
    var regulationFiles = RegulationFiles.builder()
        .bpmnFiles(List.of(
            getFileFromClasspath("registry-regulation/correct/bp-grouping/bpmn/process_for_group_1.bpmn"),
            getFileFromClasspath("registry-regulation/correct/bp-grouping/bpmn/process_for_group_2.bpmn")))
        .bpGroupingFiles(Collections.singleton(
            getFileFromClasspath("registry-regulation/broken/bp-grouping/bp-grouping-process-definition-id-duplicates.yml")))
        .build();

    var errors = validator.validate(regulationFiles,
        ValidationContext.of(RegulationFileType.BP_GROUPING_TO_BPMN));

    assertThat(errors, is(not(empty())));
  }

  @Test
  void shouldFailBpGroupingProcessDefinitionIdNotExistValidation() {
    var regulationFiles = RegulationFiles.builder()
        .bpmnFiles(List.of(
            getFileFromClasspath("registry-regulation/correct/bp-grouping/bpmn/process_for_group_1.bpmn"),
            getFileFromClasspath("registry-regulation/correct/bp-grouping/bpmn/process_for_group_2.bpmn")))
        .bpGroupingFiles(Collections.singleton(
            getFileFromClasspath("registry-regulation/broken/bp-grouping/bp-grouping-process-definition-id-not-exist.yml")))
        .build();

    var errors = validator.validate(regulationFiles,
        ValidationContext.of(RegulationFileType.BP_GROUPING_TO_BPMN));

    assertThat(errors, is(not(empty())));
  }

  private File getFileFromClasspath(String filePath) {
    var classLoader = getClass().getClassLoader();
    return new File(Objects.requireNonNull(classLoader.getResource(filePath)).getFile());
  }
}