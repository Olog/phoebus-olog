/*
 * Copyright (C) 2021 European Spallation Source ERIC.
 */

package org.phoebus.olog.docker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.State;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests for Olog and Elasticsearch that make use of existing dockerization
 * with docker-compose.yml / Dockerfile.
 *
 * <p>
 * Focus of this class is to have Olog and Elasticsearch up and running together with usage of
 * {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.olog.LogbooksResource
 */
@Testcontainers
public class OlogLogbooksIT {

    // Note
    //     ------------------------------------------------------------------------------------------------
    //     About
    //         requires
    //             elastic indices for Olog, ensured at start-up
    //             environment
    //                 default ports, 8080 for Olog, 9200 for Elasticsearch
    //                 demo_auth enabled
    //         json
    //             check(s) for json and objects written as json
    //             objects representing data/entities sent/received - serialized/deserialized
    //         docker containers shared for tests
    //             each test to leave Olog, Elasticsearch in clean state - not disturb other tests
    //             clean state may be content with status inactive
    //         each test uses multiple endpoints in Olog API
    //     ------------------------------------------------------------------------------------------------
    //     Olog - Service Documentation
    //         https://olog.readthedocs.io/en/latest/
    //     ------------------------------------------------------------------------------------------------
    //     OLOG API                                     LogbooksResource
    //     --------------------                         --------------------
    //     Retrieve a Logbook    .../logbooks/<name>    (GET)       findByTitle(String)
    //     List Logbooks         .../logbooks           (GET)       findAll()
    //     Create a Logbook      .../logbooks/<name>    (PUT)       createLogbook(String, Logbook, Principal)
    //     Create Logbooks       .../logbooks           (PUT)       updateLogbooks(List<Logbook>)
    //     Remove Logbook        .../logbooks/<name>    (DELETE)    deleteLogbook(String)
    //     ------------------------------------------------------------------------------------------------

    static final String LOGBOOKS = "/logbooks";

    static final String HTTP_IP_PORT_OLOG_LOGBOOKS            = ITUtil.HTTP +                           ITUtil.IP_PORT_OLOG + LOGBOOKS;
    static final String HTTP_AUTH_USER_IP_PORT_OLOG_LOGBOOKS  = ITUtil.HTTP + ITUtil.AUTH_USER  + "@" + ITUtil.IP_PORT_OLOG + LOGBOOKS;
    static final String HTTP_AUTH_ADMIN_IP_PORT_OLOG_LOGBOOKS = ITUtil.HTTP + ITUtil.AUTH_ADMIN + "@" + ITUtil.IP_PORT_OLOG + LOGBOOKS;

    // test data
    //     logbooks l1 - l10, owner admin, state Active - Inactive
    //     logbooks l1 - l2   owner admin, state Inactive

    static Logbook logbook_l1_owner_a_state_a;
    static Logbook logbook_l2_owner_a_state_a;
    static Logbook logbook_l3_owner_a_state_a;
    static Logbook logbook_l4_owner_a_state_a;
    static Logbook logbook_l5_owner_a_state_a;

    static Logbook logbook_l1_owner_a_state_i;
    static Logbook logbook_l2_owner_a_state_i;
    static Logbook logbook_l6_owner_a_state_i;
    static Logbook logbook_l7_owner_a_state_i;
    static Logbook logbook_l8_owner_a_state_i;
    static Logbook logbook_l9_owner_a_state_i;
    static Logbook logbook_l10_owner_a_state_i;

    @Container
    public static final DockerComposeContainer<?> ENVIRONMENT =
        new DockerComposeContainer<>(new File("docker-compose.yml"))
            .waitingFor(ITUtil.OLOG, Wait.forLogMessage(".*Started Application.*", 1));

    @BeforeAll
    public static void setupObjects() {
        logbook_l1_owner_a_state_a  = new Logbook("l1",  "admin", State.Active);
        logbook_l2_owner_a_state_a  = new Logbook("l2",  "admin", State.Active);
        logbook_l3_owner_a_state_a  = new Logbook("l3",  "admin", State.Active);
        logbook_l4_owner_a_state_a  = new Logbook("l4",  "admin", State.Active);
        logbook_l5_owner_a_state_a  = new Logbook("l5",  "admin", State.Active);

        logbook_l1_owner_a_state_i  = new Logbook("l1",  "admin", State.Inactive);
        logbook_l2_owner_a_state_i  = new Logbook("l2",  "admin", State.Inactive);
        logbook_l6_owner_a_state_i  = new Logbook("l6",  "admin", State.Inactive);
        logbook_l7_owner_a_state_i  = new Logbook("l7",  "admin", State.Inactive);
        logbook_l8_owner_a_state_i  = new Logbook("l8",  "admin", State.Inactive);
        logbook_l9_owner_a_state_i  = new Logbook("l9",  "admin", State.Inactive);
        logbook_l10_owner_a_state_i = new Logbook("l10", "admin", State.Inactive);
    }

    @AfterAll
    public static void tearDownObjects() {
        logbook_l1_owner_a_state_a  = null;
        logbook_l2_owner_a_state_a  = null;
        logbook_l3_owner_a_state_a  = null;
        logbook_l4_owner_a_state_a  = null;
        logbook_l5_owner_a_state_a  = null;

        logbook_l1_owner_a_state_i  = null;
        logbook_l2_owner_a_state_i  = null;
        logbook_l6_owner_a_state_i  = null;
        logbook_l7_owner_a_state_i  = null;
        logbook_l8_owner_a_state_i  = null;
        logbook_l9_owner_a_state_i  = null;
        logbook_l10_owner_a_state_i = null;
    }

    @Test
    public void ologUp() {
        try {
            String address = ITUtil.HTTP_IP_PORT_OLOG;
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    public void handleLogbookRetrieveCheck() {
        // what
        //     check(s) for retrieve logbook
        //         e.g.
        //             retrieve non-existing logbook
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Logbook
        //         List Logbooks
        //         Create a Logbook
        //         Create Logbooks
        //         Remove Logbook

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l11");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_NOT_FOUND);
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    public void handleLogbookRemoveCheck() {
        // what
        //     check(s) for remove logbook
        //         e.g.
        //             remove non-existing logbook
        //     --------------------------------------------------------------------------------
        //         Retrieve a Logbook
        //         List Logbooks
        //         Create a Logbook
        //         Create Logbooks
        //     x   Remove Logbook

        try {
            // might be both 401, 404
            //     401 UNAUTHORIZED
            //     404 NOT_FOUND
            String[] response = ITUtil.runShellCommand(deleteCurlLogbookForUser("l11"));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_NOT_FOUND);

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l11"));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_NOT_FOUND);
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    public void handleLogbookCreateCheckJson() {
        // what
        //     check(s) for create logbook
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 state      - null (empty, incorrect value (ok: Active, Inactive))
        //     --------------------------------------------------------------------------------
        //         Retrieve a Logbook
        //     x   List Logbooks
        //     x   Create a Logbook
        //         Create Logbooks
        //         Remove Logbook

        String json_incomplete1 = "{\"incomplete\"}";
        String json_incomplete2 = "{\"incomplete\"";
        String json_incomplete3 = "{\"incomplete}";
        String json_incomplete4 = "{\"\"}";
        String json_incomplete5 = "{incomplete\"}";
        String json_incomplete6 = "\"incomplete\"}";
        String json_incomplete7 = "{";
        String json_incomplete8 = "}";
        String json_incomplete9 = "\"";

        String json_logbook_l1_name_na     = "{\"na\":\"l1\",\"owner\":\"admin\",\"state\":\"Active\"}";

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", json_incomplete1));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", json_incomplete2));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", json_incomplete3));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", json_incomplete4));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", json_incomplete5));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", json_incomplete6));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", json_incomplete7));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", json_incomplete8));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", json_incomplete9));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", json_logbook_l1_name_na));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    public void handleLogbookCreateCheck() {
        // what
        //     check(s) for create logbook
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 state      - null (empty, incorrect value (ok: Active, Inactive))
        //     --------------------------------------------------------------------------------
        //         Retrieve a Logbook
        //     x   List Logbooks
        //     x   Create a Logbook
        //         Create Logbooks
        //         Remove Logbook

        Logbook logbook_check = new Logbook();

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            // response = ITUtil.runShellCommand(createCurlLogbookForUser("l1", mapper.writeValueAsString(logbook_l1_owner_a_state_a)));
            // ITUtil.assertResponseLength2Code(HttpURLConnection.HTTP_UNAUTHORIZED, response);

            response = ITUtil.runShellCommand(createCurlLogbookForUser("asdf", mapper.writeValueAsString(logbook_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("asdf", mapper.writeValueAsString(logbook_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            logbook_check.setName(null);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("asdf", mapper.writeValueAsString(logbook_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            logbook_check.setName("");

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("asdf", mapper.writeValueAsString(logbook_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    public void handleLogbook() {
        // what
        //     user with required role
        //     create tag
        //     --------------------------------------------------------------------------------
        //     list, create, list/retrieve, remove (unauthorized), remove, retrieve/list
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Logbook
        //     x   List Logbooks
        //     x   Create a Logbook
        //         Create Logbooks
        //     x   Remove Logbook

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", mapper.writeValueAsString(logbook_l1_owner_a_state_a)));
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(logbook_l1_owner_a_state_a.equals(mapper.readValue(response[1], Logbook.class)));

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsLogbooks(
                    mapper.readValue(response[1], Logbook[].class),
                    logbook_l1_owner_a_state_a);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(logbook_l1_owner_a_state_a.equals(mapper.readValue(response[1], Logbook.class)));

            // response = ITUtil.runShellCommand(deleteCurlLogbookForUser("l1"));
            // ITUtil.assertResponseLength2Code(HttpURLConnection.HTTP_UNAUTHORIZED, response);

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l1"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(logbook_l1_owner_a_state_i.equals(mapper.readValue(response[1], Logbook.class)));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    public void handleLogbook2() {
        // what
        //     create logbooks, one by one
        //     --------------------------------------------------------------------------------
        //     list, create (2), list/retrieve, remove, list/retrieve, remove, retrieve/list
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Logbook
        //     x   List Logbooks
        //     x   Create a Logbook
        //         Create Logbooks
        //     x   Remove Logbook

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", mapper.writeValueAsString(logbook_l1_owner_a_state_a)));
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(logbook_l1_owner_a_state_a.equals(mapper.readValue(response[1], Logbook.class)));

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l2", mapper.writeValueAsString(logbook_l2_owner_a_state_a)));
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(logbook_l2_owner_a_state_a.equals(mapper.readValue(response[1], Logbook.class)));

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsLogbooks(
                    mapper.readValue(response[1], Logbook[].class),
                    logbook_l1_owner_a_state_a,
                    logbook_l2_owner_a_state_a);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(logbook_l1_owner_a_state_a.equals(mapper.readValue(response[1], Logbook.class)));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l2");
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(logbook_l2_owner_a_state_a.equals(mapper.readValue(response[1], Logbook.class)));

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l1"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsLogbooks(
                    mapper.readValue(response[1], Logbook[].class),
                    logbook_l2_owner_a_state_a);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(logbook_l1_owner_a_state_i.equals(mapper.readValue(response[1], Logbook.class)));

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l2"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l2");
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(logbook_l2_owner_a_state_i.equals(mapper.readValue(response[1], Logbook.class)));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    public void handleLogbook3ChangeState() {
        // what
        //     replace logbook, change state
        //     --------------------------------------------------------------------------------
        //     list, create, list/retrieve, update, list/retrieve, remove, retrieve/list
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Logbook
        //     x   List Logbooks
        //     x   Create a Logbook
        //         Create Logbooks
        //     x   Remove Logbook

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", mapper.writeValueAsString(logbook_l1_owner_a_state_a)));
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(logbook_l1_owner_a_state_a.equals(mapper.readValue(response[1], Logbook.class)));

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsLogbooks(
                    mapper.readValue(response[1], Logbook[].class),
                    logbook_l1_owner_a_state_a);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(logbook_l1_owner_a_state_a.equals(mapper.readValue(response[1], Logbook.class)));

            response = ITUtil.runShellCommand(createCurlLogbookForAdmin("l1", mapper.writeValueAsString(logbook_l1_owner_a_state_i)));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(logbook_l1_owner_a_state_i.equals(mapper.readValue(response[1], Logbook.class)));

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l1"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(logbook_l1_owner_a_state_i.equals(mapper.readValue(response[1], Logbook.class)));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    public void handleLogbooksCreateCheck() {
        // what
        //     check(s) for create logbooks
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 state      - null, (empty, incorrect value (ok: Active, Inactive))
        //     --------------------------------------------------------------------------------
        //         Retrieve a Logbook
        //     x   List Logbooks
        //         Create a Logbook
        //     x   Create Logbooks
        //         Remove Logbook

        Logbook logbook_check = new Logbook();

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            Logbook[] logbooks = new Logbook[] {
                    logbook_l1_owner_a_state_a,
                    logbook_l2_owner_a_state_a,
                    logbook_l3_owner_a_state_a,
                    logbook_l4_owner_a_state_a,
                    logbook_l5_owner_a_state_a,
                    logbook_l6_owner_a_state_i,
                    logbook_l7_owner_a_state_i,
                    logbook_l8_owner_a_state_i,
                    logbook_l9_owner_a_state_i,
                    logbook_l10_owner_a_state_i,
                    logbook_check
            };

            response = ITUtil.runShellCommand(createCurlLogbooksForAdmin(mapper.writeValueAsString(logbooks)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            logbook_check.setName(null);
            logbooks[10] = logbook_check;

            response = ITUtil.runShellCommand(createCurlLogbooksForAdmin(mapper.writeValueAsString(logbooks)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            logbook_check.setName("");
            logbooks[10] = logbook_check;

            response = ITUtil.runShellCommand(createCurlLogbooksForAdmin(mapper.writeValueAsString(logbooks)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    public void handleLogbooks() {
        // what
        //     create logbooks
        //     --------------------------------------------------------------------------------
        //     list, create (10), list/retrieve, delete (5), list/retrieve, delete (5), retrieve/list
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Logbook
        //     x   List Logbooks
        //         Create a Logbook
        //     x   Create Logbooks
        //     x   Remove Logbook

        Logbook[] logbooks_active_inactive = new Logbook[] {
                logbook_l1_owner_a_state_a,
                logbook_l2_owner_a_state_a,
                logbook_l3_owner_a_state_a,
                logbook_l4_owner_a_state_a,
                logbook_l5_owner_a_state_a,
                logbook_l6_owner_a_state_i,
                logbook_l7_owner_a_state_i,
                logbook_l8_owner_a_state_i,
                logbook_l9_owner_a_state_i,
                logbook_l10_owner_a_state_i
        };

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.runShellCommand(createCurlLogbooksForAdmin(mapper.writeValueAsString(logbooks_active_inactive)));
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsLogbooks(
                    mapper.readValue(response[1], Logbook[].class),
                    logbooks_active_inactive);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsLogbooks(
                    mapper.readValue(response[1], Logbook[].class),
                    logbook_l1_owner_a_state_a,
                    logbook_l2_owner_a_state_a,
                    logbook_l3_owner_a_state_a,
                    logbook_l4_owner_a_state_a,
                    logbook_l5_owner_a_state_a);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(logbook_l1_owner_a_state_a, mapper.readValue(response[1], Logbook.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l2");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(logbook_l2_owner_a_state_a, mapper.readValue(response[1], Logbook.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l3");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(logbook_l3_owner_a_state_a, mapper.readValue(response[1], Logbook.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l4");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(logbook_l4_owner_a_state_a, mapper.readValue(response[1], Logbook.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l5");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(logbook_l5_owner_a_state_a, mapper.readValue(response[1], Logbook.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l6");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(logbook_l6_owner_a_state_i, mapper.readValue(response[1], Logbook.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l7");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(logbook_l7_owner_a_state_i, mapper.readValue(response[1], Logbook.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l8");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(logbook_l8_owner_a_state_i, mapper.readValue(response[1], Logbook.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l9");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(logbook_l9_owner_a_state_i, mapper.readValue(response[1], Logbook.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS + "/l10");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(logbook_l10_owner_a_state_i, mapper.readValue(response[1], Logbook.class));

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l1"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l2"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l3"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l9"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l10"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsLogbooks(
                    mapper.readValue(response[1], Logbook[].class),
                    logbook_l4_owner_a_state_a,
                    logbook_l5_owner_a_state_a);

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l4"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l5"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l6"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l7"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlLogbookForAdmin("l8"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGBOOKS);
            ITUtil.assertResponseLength2CodeOK(response);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Utility method to return curl to create logbook for regular user.
     *
     * @param logbookName logbook name
     * @param logbookJson logbook json
     * @return curl to create logbook
     */
    private static String createCurlLogbookForUser(String logbookName, String logbookJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + OlogLogbooksIT.HTTP_AUTH_USER_IP_PORT_OLOG_LOGBOOKS + "/" + logbookName + " -d '" + logbookJson + "'";
    }

    /**
     * Utility method to return curl to create logbook for admin user.
     *
     * @param logbookName logbook name
     * @param logbookJson logbook json
     * @return curl to create logbook
     */
    private static String createCurlLogbookForAdmin(String logbookName, String logbookJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + OlogLogbooksIT.HTTP_AUTH_ADMIN_IP_PORT_OLOG_LOGBOOKS + "/" + logbookName + " -d '" + logbookJson + "'";
    }

    /**
     * Utility method to return curl to create logbooks for admin user.
     *
     * @param logbooksJson logbooks json
     * @return curl to create logbooks
     */
    private static String createCurlLogbooksForAdmin(String logbooksJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + HTTP_AUTH_ADMIN_IP_PORT_OLOG_LOGBOOKS + " -d '" + logbooksJson + "'";
    }

    /**
     * Utility method to return curl to delete logbook for regular user.
     *
     * @param logbookName logbook name
     * @return curl to delete logbook
     */
    private static String deleteCurlLogbookForUser(String logbookName) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XDELETE -i " + HTTP_AUTH_USER_IP_PORT_OLOG_LOGBOOKS + "/" + logbookName;
    }

    /**
     * Utility method to return curl to delete logbook for admin user.
     *
     * @param logbookName logbook name
     * @return curl to delete logbook
     */
    private static String deleteCurlLogbookForAdmin(String logbookName) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XDELETE -i " + HTTP_AUTH_ADMIN_IP_PORT_OLOG_LOGBOOKS + "/" + logbookName;
    }

}
