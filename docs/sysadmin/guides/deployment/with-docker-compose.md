# Deploying with Docker Compose

## Prerequisites

- Docker Compose
- Adequate 'mmap count' for ElasticSearch: <https://www.elastic.co/guide/en/elasticsearch/reference/current/vm-max-map-count.html>
  - On Linux, you can run `sysctl -w vm.max_map_count=262144` to set this
- Adequate memory lock limit for ElasticSearch
  - You can check this with `ulimit -l` - you can set it to `unlimited` but the command varies

## Run

From the `phoebus-olog` repository:

- Build the image: `docker-compose build`
- Run the containers: `docker-compose up`

## Check if service is running

Once the service is running, the service consists of a welcome page `http://localhost:8080/Olog` 

```console
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

```bash
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
