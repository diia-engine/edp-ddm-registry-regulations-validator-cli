package com.epam.digital.data.platform.registry.regulation.validation.cli.validator.report;

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

class ReportRoleExistenceValidatorTest {

  private ReportRoleExistenceValidator reportRoleExistenceValidator;

  @BeforeEach
  public void setUp() {
    List<String> defaultRoles = List.of("officer", "citizen");
    String officerPermissionsFileName = "officer.yml";
    this.reportRoleExistenceValidator =
        new ReportRoleExistenceValidator(new YAMLMapper(), officerPermissionsFileName,
            defaultRoles);
  }

  @Test
  void shouldPassReportRoleExistenceValidator() {
    var regulationFiles = RegulationFiles.builder()
        .reportsFolders(
            Collections.singleton(getFileFromClasspath("registry-regulation/correct/reports")))
        .rolesFiles(Set.of(getFileFromClasspath("registry-regulation/correct/officer.yml")))
        .build();
    var errors = reportRoleExistenceValidator.validate(regulationFiles,
        ValidationContext.of(RegulationFileType.REPORT_ROLE_EXISTENCE));
    assertThat(errors, is(empty()));
  }

  @Test
  void shouldFailReportRoleExistenceValidationDueToNonExistentRole() {
    var regulationFiles = RegulationFiles.builder()
        .reportsFolders(
            Collections.singleton(getFileFromClasspath("registry-regulation/broken/reports")))
        .rolesFiles(Set.of(getFileFromClasspath("registry-regulation/correct/officer.yml")))
        .build();

    var errors = reportRoleExistenceValidator.validate(regulationFiles,
        ValidationContext.of(RegulationFileType.REPORT_ROLE_EXISTENCE));

    assertThat(errors, is(not(empty())));
  }

  private File getFileFromClasspath(String filePath) {
    var classLoader = getClass().getClassLoader();
    return new File(classLoader.getResource(filePath).getFile());
  }

}