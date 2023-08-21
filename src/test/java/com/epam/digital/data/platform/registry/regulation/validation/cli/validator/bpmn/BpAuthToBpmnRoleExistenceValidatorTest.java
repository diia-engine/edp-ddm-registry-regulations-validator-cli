package com.epam.digital.data.platform.registry.regulation.validation.cli.validator.bpmn;

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
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BpAuthToBpmnRoleExistenceValidatorTest {

  private BpAuthToBpmnRoleExistenceValidator bpAuthValidator;

  @BeforeEach
  public void setUp() {
    List<String> defaultRoles = List.of("officer", "citizen");
    this.bpAuthValidator = new BpAuthToBpmnRoleExistenceValidator(new YAMLMapper(), defaultRoles);
  }

  @Test
  void shouldPassBpAuthToBpmnRoleExistenceValidator() {
    var regulationFiles = RegulationFiles.builder()
        .bpAuthFiles(
            Collections.singleton(getFileFromClasspath("registry-regulation/correct/bp-auth.yml")))
        .rolesFiles(Set.of(getFileFromClasspath("registry-regulation/correct/citizen.yml"),
            getFileFromClasspath("registry-regulation/correct/officer.yml")))
        .build();

    var errors = bpAuthValidator.validate(regulationFiles,
        ValidationContext.of(RegulationFileType.BP_ROLE_EXISTENCE));

    assertThat(errors, is(empty()));
  }

  @Test
  void shouldFailBpAuthToBpmnRoleExistenceDueToNonExistentRole() {
    var regulationFiles = RegulationFiles.builder()
        .bpAuthFiles(Collections.singleton(
            getFileFromClasspath("registry-regulation/broken/bp-auth-expected-role.yml")))
        .rolesFiles(Set.of(getFileFromClasspath("registry-regulation/correct/citizen.yml"),
            getFileFromClasspath("registry-regulation/correct/officer.yml")))
        .build();

    var errors = bpAuthValidator.validate(regulationFiles,
        ValidationContext.of(RegulationFileType.BP_ROLE_EXISTENCE));

    assertThat(errors, is(not(empty())));
  }

  private File getFileFromClasspath(String filePath) {
    var classLoader = getClass().getClassLoader();
    return new File(classLoader.getResource(filePath).getFile());
  }

}