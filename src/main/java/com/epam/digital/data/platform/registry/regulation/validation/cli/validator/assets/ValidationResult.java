package com.epam.digital.data.platform.registry.regulation.validation.cli.validator.assets;

import lombok.Getter;

@Getter
public class ValidationResult {

  private final boolean valid;
  private final String errorMessage;

  public ValidationResult(boolean valid, String errorMessage) {
    this.valid = valid;
    this.errorMessage = errorMessage;
  }

}
