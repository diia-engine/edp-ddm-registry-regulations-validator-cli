/*
 * Copyright 2022 EPAM Systems.
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

package com.epam.digital.data.platform.registry.regulation.validation.cli.support;

public enum CommandLineArg {

  GLOBAL_VARS("global-vars-files"),
  BP_AUTH("bp-auth-files"),
  BP_TREMBITA("bp-trembita-files"),
  BP_TREMBITA_CONFIG("bp-trembita-config"),
  ROLES("roles-files"),
  BPMN("bpmn-files"),
  DMN("dmn-files"),
  FORMS("form-files"),
  DATAFACTORY_SETTINGS("datafactory-settings-files"),
  REGISTRY_SETTINGS("registry-settings-files"),
  LIQUIBASE("liquibase-files"),
  EXCERPTS("excerpt-folders");

  private final String argOptionName;

  CommandLineArg(String argOptionName) {
    this.argOptionName = argOptionName;
  }

  public String getArgOptionName() {
    return argOptionName;
  }
}
