# Building

## Prerequisites

- JDK 25 or newer
- Maven

## Configuration (optional)

Before building Phoebus Olog,
you can configure the service
by editing configuration files under `src/main/resources`.

## Building a deployable jar

To build the project with all dependencies,
including the Tomcat server,
run:

```bash
mvn -Pdeployable-jar clean install
```
