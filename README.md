# Remove maven package from AWS codeartifact

This repository is a maven plugin to allows delete maven package from AWS codeartifact before deploy the maven package version.

## Getting Started

To install this plugin locally, run the following commands:

<br>

```bash
git clone https://github.com/Fred78290/create-ecr-repository-maven-plugin.git
cd create-ecr-repository-maven-plugin
mvn clean install
```

The documentation plugin is available [here](https://fred78290.github.io/create-ecr-repository-maven-plugin/)

## Usage in POM

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>com.aldunelabs.maven.ecr</groupId>
                <artifactId>create-ecr-repository-maven-plugin</artifactId>
                <version>1.0.0</version>
                <configuration>
                    <region>${env.AWS_REGION}</region>
                    <profile>${env.AWS_PROFILE}</profile>
                    <accesskey>${env.AWS_ACCESSKEY}</accesskey>
                    <secretkey>${env.AWS_SECRETKEY}</secretkey>
                    <token>${env.AWS_SESSIONTOKEN}</token>
                    <repository>${env.ECR_REPOSITORY}</repository>
                    <mutable>true|false|${env.ECR_MUTABLE}</mutable>
                    <registryId>${env.ECR_REGISTRY_ID}</registryId>
                    <lifecycleUrl>URL to lifecycle policy</lifecycleUrl>
                    <permissionsUrl>URL to permission policy</permissionsUrl>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

## Plugin repository

```xml
    <pluginRepositories>
        <pluginRepository>
            <id>com.aldunelabs.maven.codeartifact</id>
            <url>https://maven.pkg.github.com/Fred78290/delete-codeartifact-maven-plugin</url>
            </pluginRepository>
    </pluginRepositories>
```
