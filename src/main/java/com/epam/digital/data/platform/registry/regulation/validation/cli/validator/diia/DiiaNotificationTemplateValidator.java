package com.epam.digital.data.platform.registry.regulation.validation.cli.validator.diia;

import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.RegulationValidator;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationContext;
import com.epam.digital.data.platform.registry.regulation.validation.cli.validator.ValidationError;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class DiiaNotificationTemplateValidator implements RegulationValidator<File> {

  private static final String DIIA_NOTIFICATION_TEMPLATE_FILE_NAME = "notification.diia";
  private static final String DIIA_NOTIFICATION_ATTRIBUTES_FILE_NAME = "notification.yml";

  private final RegulationValidator<File> templateFileValidator;
  private final RegulationValidator<File> attributeFileValidator;

  public DiiaNotificationTemplateValidator(RegulationValidator<File> templateFileValidator,
                                           RegulationValidator<File> attributeFileValidator) {
    this.templateFileValidator = templateFileValidator;
    this.attributeFileValidator = attributeFileValidator;
  }

  @Override
  public Set<ValidationError> validate(File templateDirectory, ValidationContext context) {
    var templateFile = new File(templateDirectory, DIIA_NOTIFICATION_TEMPLATE_FILE_NAME);
    Set<ValidationError> errors = new HashSet<>(templateFileValidator.validate(templateFile, context));

    var attributesFile = new File(templateDirectory, DIIA_NOTIFICATION_ATTRIBUTES_FILE_NAME);
    errors.addAll(attributeFileValidator.validate(attributesFile, context));

    return errors;
  }

}
