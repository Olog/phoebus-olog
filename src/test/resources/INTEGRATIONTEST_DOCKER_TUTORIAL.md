### About

Describe ability to develop and run integration tests for Olog API with Docker.

In other words, how to use `src/test/java` to test `src/main/java` with integration tests using Docker.

##### Background

Olog with Elasticsearch and MongDB together with the environment in which the applications run, is complex and usually heavily relied on by other applications and environments. Outside interface is to Olog but Olog, Elasticsearch and MongoDB go together. Therefore, there is need to test Olog, Elasticsearch and MongoDB together.

It is possible to test Olog API by running Olog, Elasticsearch and MongoDB applications together as Docker containers and executing a series of requests and commands to test their behavior. This tutorial will show how it works and give examples.

##### Content

* [Prerequisites](#prerequisites)
* [Examples](#examples)
* [How it works - big picture](#how-it-works-big-picture)
* [How it works - in detail](#how-it-works-in-detail)
* [How to run](#how-to-run)
* [Reference](#reference)

### Prerequisites

##### Tools

* Docker - engine 18.06.0+ or later, compose 2.21.0 or later, compose file version 3.7 to be supported

##### Dependencies

* JUnit 5
* Testcontainers

##### Files

* folder `src/test/java` and package `org.phoebus.olog.docker`
* [docker-compose-integrationtest.yml](docker-compose-integrationtest.yml)
* [Dockerfile.integrationtest](Dockerfile.integrationtest)

### Examples

##### Simple

[OlogIT.java](src/test/java/org/phoebus/olog/docker/OlogIT.java)

```
@Test
void ologUp()
```

Purpose
* verify that Olog is up and running

How
* Http request (GET) is run towards Olog base url and response code is verified to be 200

##### Medium

[OlogPropertiesIT.java](src/test/java/org/phoebus/olog/docker/OlogPropertiesIT.java)

```
@Test
void handleProperty()
```

Purpose
* verify behavior for single property that include commands - list, create property, list, retrieve, delete (unauthorized), delete, list

How
* a series of Http requests (GET) and curl commands (POST, PUT, DELETE) are run towards the application to test behavior

##### Complex

[OlogLogsQueryIT.java](src/test/java/org/phoebus/olog/docker/OlogLogsQueryIT.java)

```
@Test
void handleLogsQueryByPattern()
```

Purpose
* set up test fixture - properties, tags, logbooks, logs associated with properties, tags & logbooks
* query by pattern - search for a list of logs based on content, properties, tags, and/or logbooks
* tear down test fixture - reverse to set up

How
* a series of Http requests (GET) and curl commands (POST, PUT, DELETE) are run towards the application to test behavior

### How it works - big picture

Integration tests are implemented in test class annotated with `@Testcontainers`. Test class starts a docker container for the application (Olog service) and other docker containers for elastic (Elasticsearch) and mongo (MongoDB) through `docker-compose-integrationtest.yml` and `Dockerfile.integrationtest` after which JUnit tests are run.

```
@Testcontainers
class OlogIT {

    @Container
    public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

    @Test
    void ologUp() {
        try {
            int responseCode = ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_OLOG);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (Exception e) {
            fail();
        }
    }
```

Http requests (GET) and curl commands (POST, PUT, DELETE) are run towards the application to test behavior (read, list, query, create, update, remove) and replies are received and checked if content is as expected.

There are tests for properties, tags, logbooks and logs separately and in combination.

##### Note

* Docker containers (Olog, Elasticsearch and MongoDB) are shared for tests within test class. Order in which tests are run is not known. Therefore, each test is to leave Olog, Elasticsearch and MongoDB in a clean state to not disturb other tests.

### How it works - in detail

##### Anatomy of an integration test

```
@Testcontainers
class OlogPropertiesIT {

    static Property[] default_properties;
    static Property property_p1_owner_a_state_a_attributes;
    static Property property_p1_owner_a_state_i_attributes;
    static Attribute a1;

    @Container
    public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

    @BeforeAll
    public static void setupObjects() {
        default_properties = new Property[] {new Property("resource", null, State.Active, new HashSet<Attribute>())};
        default_properties[0].addAttributes(new Attribute("name", null, State.Active));
        default_properties[0].addAttributes(new Attribute("file", null, State.Active));

        a1 = new Attribute("a1", "v1", State.Active);

        property_p1_owner_a_state_a_attributes = new Property("p1", "admin", State.Active, new HashSet<Attribute>());
        property_p1_owner_a_state_a_attributes.addAttributes(a1);

        property_p1_owner_a_state_i_attributes = new Property("p1", "admin", State.Inactive, new HashSet<Attribute>());
        property_p1_owner_a_state_i_attributes.addAttributes(a1);
    }

    @AfterAll
    public static void tearDownObjects() {
        default_properties = null;

        property_p1_owner_a_state_a_attributes = null;

        property_p1_owner_a_state_i_attributes = null;

        a1 = null;
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperty() {
        // what
        //     user with required role
        //     create property
        //         list, create, list/retrieve, remove (unauthorized), remove, retrieve/list

        try {
            ITUtilProperties.assertListProperties(1, default_properties[0]);

            ITUtilProperties.assertCreateProperty("/p1", property_p1_owner_a_state_a_attributes);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilProperties.assertListProperties(2,
                    property_p1_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=false", 2,
                    property_p1_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=true", 2,
                    property_p1_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_a_state_a_attributes);

            // check permissions
            // ITUtilProperties.assertRemoveProperty(AuthorizationChoice.USER, "/p1", HttpURLConnection.HTTP_UNAUTHORIZED);

            ITUtilProperties.assertRemoveProperty("/p1");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_a_state_i_attributes);

            ITUtilProperties.assertListProperties("?inactive=false", 1,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=true", 2,
                    property_p1_owner_a_state_i_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties(1, default_properties[0]);
        } catch (Exception e) {
            fail();
        }
    }
```

##### What happens at runtime

The test environment is started with through test class annotated with `@Testcontainers` and constant `ENVIRONMENT` annotated with `@Container`. Containers are started (Ryuk, Olog, Elasticsearch, MongoDB). Then one time setup is run (method annotated with `@BeforeAll`), after which individual tests are run (methods annotated with `@Test`) after which one time tear down is run (method annotated with `@AfterAll`). Finally tasks are done and test class is closed.

Note the extensive use of test utility classes (in more detail below) in which are shared code for common tasks.

* authorization
* serialization and deserialization of properties, tags, logbooks and logs
* Http requests (GET) and curl commands (POST, PUT, DELETE) corresponding to endpoints in Olog API
* assert response

##### Examining `handleProperty`

1.  A GET request is made to Olog to list all properties and ensure that only default property is available.
2.  A PUT request is made to Olog to create the property listed by the path parameter. Request is made with ADMIN authority.
3.  A GET request is made to Elasticsearch to refresh indices.
4.  A GET request is made to Olog to list all properties and ensure there is one (given) property available with active status, beside default property.
5.  A GET request is made to Olog to list all properties not including inactive status and ensure there is one (given) property available with active status, beside default property.
6.  A GET request is made to Olog to list all properties     including inactive status and ensure there is one (given) property available with active status, beside default property.
7.  A GET request is made to Olog to retrieve property with given name.
8.  A DELETE request is made to Olog to delete property. Request is made with ADMIN authority.
9.  A GET request is made to Elasticsearch to refresh indices.
10. A GET request is made to Olog to retrieve property with given name.
11. A GET request is made to Olog to list all properties not including inactive status and ensure that only default property is available.
12. A GET request is made to Olog to list all properties     including inactive status and ensure there is one (given) property available with inactive status, beside default property.
13. A GET request is made to Olog to list all properties and ensure that only default property is available.


* 1, 4, 5, 6, 11, 12, 13 - Request corresponds to PropertiesResource method

```
    @GetMapping
    public Iterable<Property> findAll(@RequestParam(required=false) boolean inactive) {
```

* 2 - Request corresponds to PropertiesResource method

```
    @PutMapping("/{propertyName}")
    public Property createProperty(@PathVariable String propertyName,
                                   @RequestBody final Property property,
                                   @AuthenticationPrincipal Principal principal) {
```

* 3, 9 - Request corresponds to ITUtil method

```
    static void assertRefreshElasticIndices() throws IOException {
        String[] response = doGetJson(HTTP_IP_PORT_ELASTICSEARCH + "/_refresh");
```

* 7, 10 - Request corresponds to PropertiesResource method

```
    @GetMapping("/{propertyName}")
    public Property findByTitle(@PathVariable String propertyName) {
```

* 8 - Request corresponds to PropertiesResource method

```
    @DeleteMapping("/{propertyName}")
    public void deleteProperty (@PathVariable String propertyName) {
```

##### Test classes

See `src/test/java` and `org.phoebus.olog.docker`.

* files with suffix IT.java

##### Test utilities

See `src/test/java` and `org.phoebus.olog.docker`.

* files with prefix ITTestFixture
* files with prefix ITUtil

##### Test utilities - example

With the help of test utitilies, the tests themselves may be simplified and made more clear.

```
public class ITUtilLogs {

    public static Log[] assertListLogs(int expectedEqual, Log... expected) {
        return assertListLogs("", HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    public static Log[] assertListLogs(String queryString, int expectedEqual, Log... expected) {
        return assertListLogs(queryString, HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }

    /**
     * Utility method to return the list of all logs in the directory.
     *
     * @param queryString query string
     * @param expectedResponseCode expected response code
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response logs
     * @return number of logs
     */
    public static Log[] assertListLogs(String queryString, int expectedResponseCode, int expectedGreaterThanOrEqual, int expectedLessThanOrEqual, Log... expected) {
        Log[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_OLOG_LOGS + queryString);

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Log[].class);
            }
            // expected number of items in list
            //     (if non-negative number)
            //     expectedGreaterThanOrEqual <= nbr of items <= expectedLessThanOrEqual
            if (expectedGreaterThanOrEqual >= 0) {
                assertTrue(actual.length >= expectedGreaterThanOrEqual);
            }
            if (expectedLessThanOrEqual >= 0) {
                assertTrue(actual.length <= expectedLessThanOrEqual);
            }
            if (expected != null && expected.length > 0) {
                ITUtil.assertEqualsLogs(actual, expected);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }
```

Above methods can be used like shown below.

```
@Testcontainers
public class OlogLogsQueryIT {

    @Test
    void handleLogsQueryByPattern() {

            ITUtilLogs.assertListLogs("?desc", 60);
            ITUtilLogs.assertListLogs("?desc=asdf", 0);
            ITUtilLogs.assertListLogs("?desc=Initial", 2);

```

##### Note

* (Re) Build after change in `src/main/java` is needed in order for change to be tested as `Dockerfile.integrationtest` relies on built code.
* Configuration in folder `src/test/java` and package `org.phoebus.olog.docker`, e.g. urls and port numbers, is coupled to files `Dockerfile.integrationtest` and `docker-compose-integrationtest.yml` (beside `src/main/resources/application.properties`).
* Both positive and negative tests are important to ensure validation works as expected.

### How to run

See [How to run Integration test with Docker](INTEGRATIONTEST_DOCKER_RUN.md).

### Reference

##### Olog

* [Olog Service Documentation](https://olog.readthedocs.io/en/latest/)

##### Testcontainers

* [Testcontainers](https://testcontainers.com/)
