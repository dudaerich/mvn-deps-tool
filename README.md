# replace-mvn-deps
Tool for replacing dependencies in maven project

## Usage
```
groovy replace-mvn-deps.groovy <maven project> <file with maven dependencies>
```

* \<maven project\> - path to the maven project for example: ~/Projects/projectName
* \<file with maven dependencies\> - file which contains replacing dependencies in the format _groupId:artifactId:version_

## How it works
* The tool traverses all pom.xml files in your maven project and it parses all dependencies from these poms.
* Dependencies are searched in _/project/dependencies_ and _/project/dependencyManagement/dependencies_.
* Dependencies witout version are skipped, because version is inherited from dependencyManagement.
* Tool reads \<file with maven dependencies\> and it tries to find each dependency in the project. If the dependency is found, the tool modifies version of the dependency in all occurences.
* Finally all changes are persisted to the disc.

## Limitation
* All poms must be valid XML files.
