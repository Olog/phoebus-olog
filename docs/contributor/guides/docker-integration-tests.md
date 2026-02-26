# Integration tests with Docker containers

Purpose is to have integration tests for Olog API with Docker.

See `src/test/java` and package
* `org.phoebus.olog.docker`

Integration tests start docker containers for Olog, Elasticsearch and MongoDB, and run http requests (GET) and curl commands (POST, PUT, DELETE) towards the application to test behavior (read, list, query, create, update, remove) and replies are received and checked if content is as expected.

There are tests for properties, tags, logbooks and logs separately and in combination.

Integration tests can be run in IDE and via Maven.

```
mvn failsafe:integration-test -DskipITs=false -Pintegrationtest-docker
```

See
* [How to run Integration test with Docker](src/test/resources/INTEGRATIONTEST_DOCKER_RUN.md)
* [Tutorial for Integration test with Docker](src/test/resources/INTEGRATIONTEST_DOCKER_TUTORIAL.md)

