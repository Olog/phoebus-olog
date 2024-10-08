# Olog   [![Build Java Binary and Run Tests](https://github.com/Olog/phoebus-olog/actions/workflows/java-build-and-test.yml/badge.svg)](https://github.com/Olog/phoebus-olog/actions/workflows/java-build-and-test.yml)

An online logbook service that allows for the creation and retrieval of log entries.

[Phoebus Olog Documentation](https://olog.readthedocs.io/)

### Installation
Olog 

* Prerequisites

  * JDK 17 or newer
  * Elastic version 8.2.x
  * mongo gridfs


 **Download links for the prerequisites**   
 Download and install elasticsearch (verision 8.2.x) from [elastic.com](https://www.elastic.co/downloads/past-releases/elasticsearch-8-2-3)    
 Download and install mongodb from [mongodb](https://www.mongodb.com/download-center/community)    
  
  
* Configure the service (optional)
The configuration files for olog-es are present under `phoebus-olog/tree/master/src/main/resources` 


* Build 
```
cd phoebus-olog
mvn clean install
``` 

* Build deployable jar

To build a jar with dependencies and Tomcat server, use Maven profile `deployable-jar`, e.g.:
```
cd phoebus-olog
mvn -Pdeployable-jar clean install
```

#### Start the service  

Using spring boot  

```
mvn org.springframework.boot:spring-boot-maven-plugin:run
```

#### Check if service is running

Once the service is running, the service consists of a welcome page `http://localhost:8080/Olog` 

```
$ curl http://localhost:8080/Olog
{
  "name" : "Olog Service",
  "version" : "5.0.0-SNAPSHOT",
  "elastic" : {
    "status" : "Connected",
    "clusterName" : "elastic-nasa",
    "clusterUuid" : "QNeYpFlWRueYPH3uXGUiGw",
    "version" : "co.elastic.clients.elasticsearch._types.ElasticsearchVersionInfo@79c2137f",
    "elasticHost" : "localhost",
    "elasticPort" : "9200"
  },
  "mongoDB" : "{type=STANDALONE, servers=[{address=localhost:27017, type=STANDALONE, roundTripTime=1.2 ms, state=CONNECTED}]",
  "serverConfig" : {
    "maxFileSize" : 50.0,
    "maxRequestSize" : 100.0
  }
}
```

This will provide information about the version of the Olog service running,
along with information about the version and connection status of the elastic and mongo
backends.

An example for testing the creation of a single test log entry with the demo credentials 

```
curl --location --insecure --request PUT 'https://localhost:8181/Olog/logs' \
--header 'Content-Type: application/json' \
--header 'Authorization: Basic YWRtaW46YWRtaW5QYXNz' \
--data '{
             "owner": "test-owner",
             "source": "This is an test entry",
             "title": "Tes title",
             "level": "Info",
             "logbooks": [{"name": "operations","owner": "olog-logs"}]
         }'
```

### Running using Docker Compose

**Prerequisites**

* Docker Compose
* Adequate 'mmap count' for ElasticSearch: https://www.elastic.co/guide/en/elasticsearch/reference/current/vm-max-map-count.html
  * On linux, you can run `sysctl -w vm.max_map_count=262144` to set this
* Adequate memory lock limit for ElasticSearch
  * You can check this with `ulimit -l` - you can set it to `unlimited` but the command varies

**Run**

* Build the image: `docker-compose build`
* Run the containers: `docker-compose up`

### Integration tests with Docker containers

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

### Release Olog Server binaries to maven central

The Phoebus Olog service uses the maven release plugin to prepare the publish the olog server binaries to maven central
using the sonatype repositories.

**Setup**

Create a sonatype account and update the maven settings.xml file with your sonatype credentials

```
  <servers>
   <server>
      <id>phoebus-releases</id>
      <username>shroffk</username>
      <password>*******</password>
   </server>
  </servers>
```

**Prepare the release**  
`mvn release:prepare`  
In this step will ensure there are no uncommitted changes, ensure the versions number are correct, tag the scm, etc..
A full list of checks is documented [here](https://maven.apache.org/maven-release/maven-release-plugin/examples/prepare-release.html):

**Perform the release**  
`mvn release:perform`  
Checkout the release tag, build, sign and push the build binaries to sonatype.
**NOTE:** Mac OS users should invoke `export GPG_TTY=$(tty)` prior to `mvn release:perform`.

**Publish**  
Open the staging repository in [sonatype](https://s01.oss.sonatype.org/#stagingRepositories) and hit the *publish* button

### Releasing Docker Images

Docker images are published to the GitHub Container Registry via GitHub actions. This is triggered by:
  - push to master
  
Images are published to the `ghcr.io/<org name>/<group id>-<artifact id>` registry under the following tags:
  - `latest`
  - `<version>`
  - `<version>-<timestamp>`

This tag information is extracted from the Maven POM (via the `help` plugin). In order to avoid defining all of this 
in the GitHub actions themselves, the generation of these tag names is delegated to shell scripts in the `scripts` folder. 

If you need to debug issues related to registry/tag names outside of GitHub's CI/CD environment, 
you can run these scripts locally:

```
# setup your environment with GitHub's enviroment vars and files
source ./scripts/setup-locally.sh

# Build the tag names from Maven etc and write them to PROJECT_INFO.txt
./scripts/write-project-info.sh

# Append the vars in PROJECT_INFO.txt to the GitHub environment file 
# (setup-locally.sh uses THE_GITHUB_ENV_FILE.txt, but in GH CI/CD 
# it is unique per run)
# This file should match PROJECT_INFO.txt
./scripts/set-github-env.sh
```

What you should see is:
  - registry and tag names are valid; e.g. are all lowercase and use dashes instead of spaces
  - registry and tag names should not be null/empty/unrelated to this project

If you would like to read up more on how passing environment variables between steps in a GitHub action works, 
please see their docs on [Job Outputs](https://docs.github.com/en/actions/using-jobs/defining-outputs-for-jobs). 
