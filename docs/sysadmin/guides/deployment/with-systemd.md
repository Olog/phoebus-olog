# Deploying with systemd

## Prerequisites

On the server, you will need:

- JDK 25 or newer
- [Elasticsearch] version 8.2.x
- [MongoDB]
- A built "deployable jar" version of Phoebus Olog,
  see {doc}`../building` for more information.

  [Elasticsearch]: https://www.elastic.co/
  [MongoDB]: https://www.mongodb.com/

## Creating the systemd service file

You can put the systemd service file in `/usr/local/lib/systemd/system/phoebus-olog.service`.

Make sure you replace everything that's inside `[]` brackets.

:::{literalinclude} phoebus-olog.service
:caption: `/usr/local/lib/systemd/system/phoebus-olog.service`
:language: dosini
:::

Make sure to create the `/etc/phoebus-olog.properties` file.

Then, enable and start the service with `systemctl enable --now phoebus-olog.service`.

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

## Configuration

To configure the Phoebus Olog service,
edit the `/etc/phoebus-olog.service` file.

For more information on how to configure,
see {doc}`../configuring/index`.
