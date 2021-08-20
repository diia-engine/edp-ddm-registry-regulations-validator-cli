/*
 * Copyright 2021 EPAM Systems.
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

package com.epam.digital.data.platform.registry.regulation.validation.cli;

import com.epam.digital.data.platform.registry.regulation.validation.cli.model.RegulationFiles;
import com.epam.digital.data.platform.registry.regulation.validation.cli.support.CommandLineArgsParser;
import com.epam.digital.data.platform.registry.regulation.validation.cli.support.CommandLineOptionsConverter;
import com.epam.digital.data.platform.registry.regulation.validation.cli.support.SystemExit;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.RegulationValidatorFactory;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;
import org.springframework.boot.CommandLineRunner;

@Slf4j
public class RegulationValidationCommandLineRunner implements CommandLineRunner {

  private final CommandLineArgsParser commandLineArgsParser;
  private final CommandLineOptionsConverter commandLineOptionsConverter;

  private final RegulationValidatorFactory registryRegulationValidatorFactory;

  private final SystemExit systemExit;

  public RegulationValidationCommandLineRunner(RegulationValidatorFactory registryRegulationValidatorFactory,
      CommandLineArgsParser commandLineArgsParser, CommandLineOptionsConverter commandLineOptionsConverter,
      SystemExit systemExit) {
    this.commandLineArgsParser = commandLineArgsParser;
    this.commandLineOptionsConverter = commandLineOptionsConverter;
    this.registryRegulationValidatorFactory = registryRegulationValidatorFactory;
    this.systemExit = systemExit;
  }

  @Override
  public void run(String... args) {
    try {
      parseAndValidate(args);
    } catch (ParseException e) {
      handleCommandLineParseException(e);
    }
  }

  private void parseAndValidate(String[] args) throws ParseException {
    var options = this.commandLineArgsParser.parse(args);

    if (options.hasOption("help")) {
      this.commandLineArgsParser.printHelp();
      this.systemExit.complete();
      return;
    }

    var regulationFiles = commandLineOptionsConverter.convert(options);
    if (regulationFiles.isEmpty()) {
      log.info("No registry regulation files to validate.");
      this.systemExit.complete();
      return;
    }

    validate(regulationFiles);
  }

  private void validate(RegulationFiles registryRegulationFiles) {
    var regulationFilesValidator = registryRegulationValidatorFactory.newRegulationFilesValidator();

    log.info("Starting registry regulation validation...");

    var errors = regulationFilesValidator.validate(registryRegulationFiles, ValidationContext.empty());

    if (errors.isEmpty()) {
      log.info("Registry regulation validation passed successfully.");
      this.systemExit.complete();
      return;
    }

    log.error("Registry validation failed due to:");
    errors.forEach(error -> log.error(error.toString()));

    systemExit.validationFailure();
  }

  private void handleCommandLineParseException(ParseException e) {
    log.error("Parsing failure" + (e.getMessage() != null ? ": " + e.getMessage() : "") + ". Find help below:");

    this.commandLineArgsParser.printHelp();
    this.systemExit.systemError();
  }
}