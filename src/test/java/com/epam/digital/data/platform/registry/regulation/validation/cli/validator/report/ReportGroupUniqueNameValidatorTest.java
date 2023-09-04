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

package com.epam.digital.data.platform.registry.regulation.validation.cli.validator.report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.epam.digital.data.platform.registry.regulation.validation.cli.model.RegulationFileType;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationContext;
import com.fasterxml.jackson.databind.json.JsonMapper;

class ReportGroupUniqueNameValidatorTest {

    private ReportGroupUniqueNameValidator validator;

    @BeforeEach
    public void setUp() {
        this.validator = new ReportGroupUniqueNameValidator(new JsonMapper());
    }

    @Test
    void shouldPassReportsGroupUniqueNameValidation() {
        var groupFiles = getFilesFromClasspath(
                "registry-regulation/correct/reports/citizen/laboratory.json," +
                        "registry-regulation/correct/reports/citizen/queries/queries.json," +
                        "registry-regulation/correct/reports/citizen/registration.json," +
                        "registry-regulation/correct/reports/officer/laboratory.json," +
                        "registry-regulation/correct/reports/officer/queries/queries.json," +
                        "registry-regulation/correct/reports/officer/registration.json");

        var errors = validator.validate(groupFiles, ValidationContext.of(RegulationFileType.REPORTS));

        assertThat(errors, is(empty()));
    }

    @Test
    void shouldFailReportsGroupUniqueNameValidation() {
        var groupFiles = getFilesFromClasspath(
                "registry-regulation/broken/reports/officer/laboratory.json," +
                        "registry-regulation/broken/reports/officer/registration.json");

        var errors = validator.validate(groupFiles, ValidationContext.of(RegulationFileType.REPORTS));

        assertThat(errors, is(not(empty())));
        assertThat(errors.iterator().next().getErrorMessage(), is("Duplicated dashboard names found: Атестовані лабораторії"));
    }

    @Test
    void shouldFailReportsQueriesGroupUniqueNameValidation() {
        var groupFiles = getFilesFromClasspath(
                "registry-regulation/broken/reports/officer/queries/queries.json," +
                        "registry-regulation/broken/reports/officer/queries/queries_2.json");

        var errors = validator.validate(groupFiles, ValidationContext.of(RegulationFileType.REPORTS));

        assertThat(errors.size(), is(2));
        assertThat(errors.iterator().next().getErrorMessage().startsWith("Duplicated query names found: "), is(true));
    }

    private Collection<File> getFilesFromClasspath(String paths) {
        var filePaths = List.of(paths.split(","));
        var classLoader = getClass().getClassLoader();
        return filePaths.stream().map(
                filePath -> new File(Objects.requireNonNull(classLoader.getResource(filePath)).getFile())).collect(
                Collectors.toList());
    }
}