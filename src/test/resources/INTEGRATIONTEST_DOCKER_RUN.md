### Prerequisites

##### Tools

* Docker - engine 18.06.0+ or later, compose 2.21.0 or later, compose file version 3.7 to be supported

##### Build Olog service

```
mvn clean install -Pdeployable-jar
```

### Run tests

##### IDE

All or individual integration tests (including methods) can be run in IDE as JUnit tests.

##### Maven

All integration tests can be run via Maven.

```
mvn failsafe:integration-test -DskipITs=false -Pintegrationtest-docker
mvn failsafe:integration-test -DskipITs=false -DskipITCoverage=false -Pintegrationtest-docker
```

Individual integration tests (classes) can also be run via Maven.

```
mvn test -Dtest=org.phoebus.olog.docker.OlogIT
mvn test -Dtest=org.phoebus.olog.docker.OlogLogbooksIT
mvn test -Dtest=org.phoebus.olog.docker.OlogLogsIT
mvn test -Dtest=org.phoebus.olog.docker.OlogLogsQueryIT
mvn test -Dtest=org.phoebus.olog.docker.OlogPropertiesIT
mvn test -Dtest=org.phoebus.olog.docker.OlogTagsIT
```

##### Code coverage

Run integration tests with property `-DskipITCoverage=false` in order to have code coverage analysis. By default, code coverage for integration tests is disabled.

After integration tests have been run, run below command to process coverage data. This applies for all and individual integration tests (including methods).

```
mvn verify -Djacoco.skip=false
```

Result is available in `target/site/jacoco` folder and includes code coverage execution data and reports.

```
index.html
jacoco.exec
jacoco.csv
jacoco.xml
```

##### Summary

To build and run all unit tests and integration tests (Docker)

```
mvn clean install test-compile failsafe:integration-test failsafe:verify --batch-mode --fail-at-end -DskipITs=false -Pdeployable-jar -Pintegrationtest-docker
```

To build and run all unit tests and integration tests (Docker) with code coverage.

```
mvn clean install test-compile failsafe:integration-test failsafe:verify --batch-mode --fail-at-end -Djacoco.skip=false -DskipITs=false -DskipITCoverage=false -Pdeployable-jar -Pintegrationtest-docker
```

### Note

##### Build

* (Re) Build after change in `src/main/java` in order for change to be tested
* `Dockerfile.integrationtest` relies on built code and not on Maven central
* Requires a deployable jar

##### Configuration

* Configuration in folder `src/test/java` and package `org.phoebus.olog.docker`, e.g. urls and port numbers, is coupled to files `Dockerfile.integrationtest` and `docker-compose-integrationtest.yml` (beside `src/main/resources/application.properties`)

##### Debug

* Docker containers can be inspected when debugging integration tests

##### Performance

* It may take a minute to run a test. This includes time to set up the test environment, perform the test and tear down the test environment. Setting up the test environment takes most of that time.
* It may take additional time to run an integration test with code coverage.
