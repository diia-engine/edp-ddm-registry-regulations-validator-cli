# registry-regulations-validation-cli

### Overview

* Command-line registry regulation validation utility for Low-code Platform

### Usage

```bash
Example: java -jar registry-regulations-validator-cli.jar
         [--help] 
         [--bp-auth-files=<arg>] [--bp-trembita-files=<arg>]
         [--bpmn-files=<arg>] [--dmn-files=<arg>] [--form-files=<arg>]
         [--global-vars-files=<arg>] [--roles-files=<arg>] 
         [--datafactory-settings-files=<arg>] [--registry-settings-files=<arg>]
         [--liquibase-files=<arg>]
Options:
    --help                     Help on utility usage
    --bp-auth-files=<arg>      BP authorization regulation files (accepts
                               multiple values separated by ',')
    --bp-trembita-files=<arg>  BP Trembita configuration regulation file
    --bp-trembita-config=<arg> BP Trembita registries configuration file
    --bpmn-files=<arg>         Business processes regulation files (accepts multiple values separated by ',')
                               (accepts multiple values separated by ',')
    --dmn-files=<arg>          Business rules regulation files (accepts
                               multiple values separated by ',')
    --form-files=<arg>         UI forms regulation files (accepts multiple
                               values separated by ',')
    --global-vars-files=<arg>   Global variables regulation files (accepts multiple values separated by ',')
    --roles-files=<arg>        Authorization roles regulation files
                               (accepts multiple values separated by ',')
    --datafactory-settings-files=<arg>     Datafactory Settings regulation files with yml, yaml extensions
    --registry-settings-files=<arg>     Registry Settings regulation files with yml, yaml extensions
    --liquibase-files=<arg>    Liquibase regulation files introduce Database change set with xml extensions
    --bp-grouping-files=<arg>  Business process grouping file
    --mock-integration-files=<arg>         Mock Integration regulation files (accepts multiple
                               values separated by ',')    
Exit codes: 0 (success), 1 (system error), 10 (validation failure)
```
### Test execution

* Tests could be run via maven command:
    * `mvn verify` OR using appropriate functions of your IDE.
    
### License

The registry-regulations-validation-cli is Open Source software released under
the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).