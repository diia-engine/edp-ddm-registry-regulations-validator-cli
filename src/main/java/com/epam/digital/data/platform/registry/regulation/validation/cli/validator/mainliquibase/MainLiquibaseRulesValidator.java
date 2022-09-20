/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.registry.regulation.validation.cli.validator.mainliquibase;

import com.deliveredtechnologies.rulebook.Fact;
import com.deliveredtechnologies.rulebook.FactMap;
import com.deliveredtechnologies.rulebook.NameValueReferableMap;
import com.deliveredtechnologies.rulebook.model.RuleBook;
import com.epam.digital.data.platform.registry.regulation.validation.cli.utils.ChangelogParser;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.RegulationValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationContext;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationError;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.FactNames;
import liquibase.change.Change;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.exception.LiquibaseException;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MainLiquibaseRulesValidator implements RegulationValidator<File> {

    private final RuleBook<Set<ValidationError>> mainLiquibaseRuleBook;

    public MainLiquibaseRulesValidator(RuleBook<Set<ValidationError>> mainLiquibaseRuleBook) {
        this.mainLiquibaseRuleBook = mainLiquibaseRuleBook;
    }

    @Override
    public Set<ValidationError> validate(File regulationFile, ValidationContext context) {
        try {
            mainLiquibaseRuleBook.run(getMainLiquibaseFacts(regulationFile, context));
            return mainLiquibaseRuleBook.getResult().get().getValue();
        } catch (LiquibaseException e) {
            return Collections.singleton(
                    ValidationError.of(context.getRegulationFileType(), regulationFile,
                            "It is not possible to parse the file " + regulationFile, e)
            );
        }
    }

    private NameValueReferableMap<Object> getMainLiquibaseFacts(File regulationFile, ValidationContext context) throws LiquibaseException {
        NameValueReferableMap<Object> mainLiquibaseFacts = new FactMap<>();
        mainLiquibaseFacts.put(new Fact<>(FactNames.DATABASE_ALL_CHANGES, getAllChanges(getDatabaseChangeLog(regulationFile))));
        mainLiquibaseFacts.put(new Fact<>(FactNames.REGULATION_FILE_TYPE, context.getRegulationFileType()));
        mainLiquibaseFacts.put(new Fact<>(FactNames.REGULATION_FILE, regulationFile));

        return mainLiquibaseFacts;
    }

    private DatabaseChangeLog getDatabaseChangeLog(File regulationFile) throws LiquibaseException {
        return ChangelogParser.parseChangeLog(regulationFile);
    }

    private List<Change> getAllChanges(DatabaseChangeLog databaseChangeLog) {
        return databaseChangeLog.getChangeSets()
                .stream()
                .flatMap(x -> x.getChanges().stream())
                .collect(Collectors.toList());
    }

}
