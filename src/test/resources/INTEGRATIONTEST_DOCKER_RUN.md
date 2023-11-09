### Prerequisites

##### Tools

* Docker - engine 18.06.0+ or later, compose 2.21.0 or later, compose file version 3.7 to be supported

##### Build ChannelFinder service

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

##### Summary

To build and run all unit tests and integration tests (Docker)

```
mvn clean install test-compile failsafe:integration-test failsafe:verify --batch-mode --fail-at-end -DskipITs=false -Pdeployable-jar -Pintegrationtest-docker
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
