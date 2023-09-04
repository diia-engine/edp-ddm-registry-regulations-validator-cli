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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;

import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.RegulationValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationContext;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

public class ReportGroupUniqueNameValidator implements RegulationValidator<Collection<File>> {

    private static final String DUPLICATED_DASHBOARD_NAME_ERROR_MSG_FORMAT = "Duplicated dashboard names found: %s";
    private static final String DUPLICATED_QUERY_NAME_ERROR_MSG_FORMAT = "Duplicated query names found: %s";
    public static final String FILE_PROCESSING_ERROR_MSG = "File processing failure";
    public static final String REPORTS_FOLDER_NAME = "reports" + File.separator;
    public static final String QUERIES_FOLDER_NAME = File.separator + "queries" + File.separator;

    private final ObjectMapper jsonObjectMapper;

    public ReportGroupUniqueNameValidator(ObjectMapper jsonObjectMapper) {
        this.jsonObjectMapper = jsonObjectMapper;
    }

    @Override
    public Set<ValidationError> validate(Collection<File> reportFiles, ValidationContext context) {
        Set<String> roles = Sets.newHashSet();
        reportFiles.forEach(
                file -> roles.add(StringUtils.substringBetween(file.getPath(), REPORTS_FOLDER_NAME, File.separator)));
        Set<ValidationError> errors = Sets.newHashSet();
        for (String role : roles) {
            Set<String> processedDashboardNames = Sets.newHashSet();
            Set<String> processedQueryNames = Sets.newHashSet();
            reportFiles.forEach(file -> {
                if (file.getPath().contains(File.separator + role + File.separator)) {
                    try {
                        if (file.getPath().contains(QUERIES_FOLDER_NAME)) {
                            validateQueries(file, processedQueryNames, errors, context);
                        } else {
                            validateDashboards(file, processedDashboardNames, errors, context);
                        }
                    } catch (IOException e) {
                        errors.add(ValidationError.of(context.getRegulationFileType(),
                                file, FILE_PROCESSING_ERROR_MSG, e));
                    }
                }
            });
        }
        return errors;
    }

    private void validateQueries(File file, Set<String> queryNames, Set<ValidationError> errors,
            ValidationContext context) throws IOException {
        getQueriesNames(file).forEach(queryName -> {
            if (!queryNames.add(queryName)) {
                errors.add(toValidationError(
                        String.format(DUPLICATED_QUERY_NAME_ERROR_MSG_FORMAT, queryName), file,
                        context));
            }
        });
    }

    private void validateDashboards(File file, Set<String> dashboardNames, Set<ValidationError> errors,
            ValidationContext context) throws IOException {
        JsonNode dashboardNameNode = jsonObjectMapper.readTree(file).get("name");
        if (dashboardNameNode != null && !dashboardNames.add(dashboardNameNode.asText())) {
            errors.add(toValidationError(
                    String.format(DUPLICATED_DASHBOARD_NAME_ERROR_MSG_FORMAT, dashboardNameNode.asText()), file,
                    context));
        }
    }

    private List<String> getQueriesNames(File reportQueryFile) throws IOException {
        JsonNode jsonNode = jsonObjectMapper.readTree(reportQueryFile);
        return StreamSupport.stream(jsonNode.get("results").spliterator(), false)
                .map(jn -> jn.get("name")).filter(jn -> jn != null).map(jn -> jn.asText())
                .collect(Collectors.toList());
    }

    private ValidationError toValidationError(String message, File regulationFile, ValidationContext context) {
        return ValidationError.builder()
                .errorMessage(message)
                .regulationFileType(context.getRegulationFileType())
                .regulationFile(regulationFile)
                .build();
    }
}
