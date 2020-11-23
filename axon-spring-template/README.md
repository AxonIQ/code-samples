# Axon Spring template 
This module's intention is to be used as a template to quickly start with a new Maven based Axon application with Spring Boot, either in Java or Kotlin

### Maven build
`pom.xml` contains basic Axon, Spring Boot and Kotlin dependencies
- `axon-spring-boot-starter` is a dependency that will provide Axon Framework integrated with Spring Boot
- `spring-boot-starter` is a base dependency for Spring Boot
- `kotlin-stdlib-jdk8` and `kotlin-reflect` are basic Kotlin dependencies, and compile support is enabled via `kotlin-maven-plugin`
- Java and Kotlin classes and files can be mixed in Java source directory
- `axon-test` and `spring-boot-starter-test` are included to provide test support

### How to use
You can simply copy this directory, rename the package as you wish. You can keep the suggested package structure where:
- API package is intended for messages like Commands, Events and Queries
- Command package is intended for your command model like aggregates
- Query package is intended for your projections.
    
