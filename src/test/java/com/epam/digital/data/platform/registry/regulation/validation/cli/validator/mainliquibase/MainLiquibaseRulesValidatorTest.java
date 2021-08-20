package com.epam.digital.data.platform.registry.regulation.validation.cli.validator.mainliquibase;

import com.epam.digital.data.platform.registry.regulation.validation.cli.model.RegulationFileType;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.AbstractRulesValidatorTest;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.RegulationValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

class MainLiquibaseRulesValidatorTest extends AbstractRulesValidatorTest {

    private RegulationValidator<File> validator;

    @BeforeEach
    public void setUp() {
        this.validator = new MainLiquibaseRulesValidator(getRuleBookRunner(
                "com.epam.digital.data.platform.registry.regulation.validation.cli.validator.mainliquibase.rules"));
    }

    @Test
    void shouldPassMainLiquibaseValidation() {
        var processFile = getFileFromClasspath("registry-regulation/correct/test-main-liquibase.xml");
        var errors = validator.validate(processFile, ValidationContext.of(RegulationFileType.LIQUIBASE));

        assertThat(errors, is(empty()));
    }

    @Test
    void shouldFailMainLiquibaseValidation() {
        var processFile = getFileFromClasspath("registry-regulation/broken/test-main-liquibase.xml");
        var errors = validator.validate(processFile, ValidationContext.of(RegulationFileType.LIQUIBASE));

        assertThat(errors, is(not(empty())));
    }

    @Test
    void shouldFailMainLiquibaseValidationDueToMissedChangeLog() {
        var processFile = getFileFromClasspath("registry-regulation/broken/test-main-liquibase-broken.xml");
        var errors = validator.validate(processFile, ValidationContext.of(RegulationFileType.LIQUIBASE));

        assertThat(errors, is(not(empty())));
    }
}