# Crunch JVM Test validator

[![Build Status](https://travis-ci.org/crunch-accounting/tests-maven-plugin.svg?branch=master)](https://travis-ci.org/crunch-accounting/tests-maven-plugin) [![codecov](https://codecov.io/gh/crunch-accounting/tests-maven-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/crunch-accounting/tests-maven-plugin)

Add this Maven plugin to your project to enable automatic validation of JVM tests against our standards:

    <plugin>
        <groupId>uk.co.crunch</groupId>
        <artifactId>crunch-tests-maven-plugin</artifactId>
    </plugin>


## Goal:

* Coalesce all our unit and integration tests around:
     * [JUnit 5](https://junit.org/junit5/) tests and [AssertJ](https://github.com/joel-costigliola/assertj-core) assertions for Java/Kotlin, or:
     * JUnit 5 tests and [Strikt](https://strikt.io/) assertions for Kotlin
* In general, use static analysis to apply and enforce our own test quality rules
* Ultimately, use whatever automation we can to make it easier to move between projects, to write efficient tests, and harder to write bad tests by mistake.

## Validations performed:

The following are regarded as fatal errors:

* JUnit 3 assertions
* JUnit 4 assertions
* JUnit 5 assertions, with the exception of `assertThrows(...)` and `assertDoesNotThrow(...)`. A log message shows how these too can be replaced with can be replaced [with AssertJ calls](https://www.baeldung.com/assertj-exception-assertion)
* Google Truth assertions
* Hamcrest assertions
* A mixture of JUnit 4 and JUnit 5 `@Test` cases, that will likely cause tests to be skipped by Maven (but not the IDE)

... unless either fixed, or an explicit override is added at either the class- or the test-method, e.g.:

    @CrunchTestValidationOverride(CrunchTestValidationOverrides.JUNIT5_ASSERTIONS)

## Warnings:

The following are considered dubious, and should be reviewed for each project:

* All JUnit 5 tests:
    * Test classes or methods needlessly `public`
    * Test methods needlessly having a `test` name prefix
* Unit tests:
    * Non-constant fields that are not any known kind of `@Mock` or `@Spy`
* Integration tests:
    * Non-constant fields that are not Spring `@Autowired`, Spring Boot `@Value`, or any known kind of `@MockBean` or `@SpyBean`
