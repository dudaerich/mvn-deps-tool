# mvn-deps-tool
Tool for replacing dependencies in maven project

## Usage
```
groovy replace-mvn-deps.groovy <command> <maven project> <file with maven dependencies>
```

* \<command\> - you can use _replace_ or _bom_, see _How it Works_ for explanation what each command does
* \<maven project\> - path to the maven project for example: ~/Projects/projectName
* \<file with maven dependencies\> - file which contains replacing dependencies in the format _groupId:artifactId:version_

## How it works
* The tool traverses all pom.xml files in your maven project and it parses all dependencies from these poms.
* Dependencies are searched in _/project/dependencies_ and _/project/dependencyManagement/dependencies_.
* Dependencies witout version are skipped, because version is inherited from dependencyManagement.
* Tool reads \<file with maven dependencies\> and it tries to find each dependency in the project. If the dependency is found, the tool modifies version of the dependency in all occurences.
* Finally if you used
  * _replace_ command - all changes are persisted to the disc (poms are replaced).
  * _bom_ command - the bom with dependencies is printed to standard output. You can use [POM manipulation extension](https://libraries.io/github/the-container-store/pom-manipulation-ext) for replacing versions of dependencies.

## Limitation
* All poms must be valid XML files.
