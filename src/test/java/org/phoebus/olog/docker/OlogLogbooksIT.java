/*
 * Copyright (C) 2021 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.olog.docker;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.phoebus.olog.docker.ITUtil.AuthorizationChoice;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.State;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class OlogLogbooksIT {

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
    //     OLOG API
    //     --------------------
    //     Retrieve a Logbook        .../logbooks/<name>        GET
    //     List Logbooks             .../logbooks               GET
    //     Create a Logbook          .../logbooks/<name>        PUT
    //     Create Logbooks           .../logbooks               PUT
    //     Remove Logbook            .../logbooks/<name>        DELETE
    //     ------------------------------------------------------------------------------------------------

    // test data
    //     logbooks l1 - l10, owner admin, state Active - Inactive
    //     logbooks l1 - l2   owner admin, state Inactive

    static Logbook[] default_logbooks;

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
    public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

    @BeforeAll
    public static void setupObjects() {
        default_logbooks = new Logbook[] {
                new Logbook("operations", "olog-logs", State.Active),
                new Logbook("controls", null, State.Active)};

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
        default_logbooks = null;

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

    @AfterAll
    public static void extractJacocoReport() {
        // extract jacoco report from container file system
        ITUtil.extractJacocoReport(ENVIRONMENT,
                ITUtil.JACOCO_TARGET_PREFIX + OlogLogbooksIT.class.getSimpleName() + ITUtil.JACOCO_TARGET_SUFFIX);
    }

    @Test
    void ologUp() {
        try {
            int responseCode = ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_OLOG);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    void handleLogbookRetrieveCheck() {
        // what
        //     check(s) for retrieve logbook
        //         e.g.
        //             retrieve non-existing logbook

        ITUtilLogbooks.assertRetrieveLogbook("/l11", HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    void handleLogbookRemoveCheck() {
        // what
        //     check(s) for remove logbook
        //         e.g.
        //             remove non-existing logbook

        // might be both 401, 404
        //     401 UNAUTHORIZED
        //     404 NOT_FOUND

        // check permissions

        ITUtilLogbooks.assertRemoveLogbook(AuthorizationChoice.USER,  "/l11", HttpURLConnection.HTTP_NOT_FOUND);
        ITUtilLogbooks.assertRemoveLogbook(AuthorizationChoice.ADMIN, "/l11", HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    void handleLogbookCreateCheckJson() {
        // what
        //     check(s) for create logbook
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 state      - null (empty, incorrect value (ok: Active, Inactive))

        String json_incomplete1 = "{\"incomplete\"}";
        String json_incomplete2 = "{\"incomplete\"";
        String json_incomplete3 = "{\"incomplete}";
        String json_incomplete4 = "{\"\"}";
        String json_incomplete5 = "{incomplete\"}";
        String json_incomplete6 = "\"incomplete\"}";
        String json_incomplete7 = "{";
        String json_incomplete8 = "}";
        String json_incomplete9 = "\"";

        String json_logbook_l1_name_na = "{\"na\":\"l1\",\"owner\":\"admin\",\"state\":\"Active\"}";

        try {
            ITUtilLogbooks.assertListLogbooks(2,
                    default_logbooks[1],
                    default_logbooks[0]);

            ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.ADMIN, "/l1", json_incomplete1, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.ADMIN, "/l1", json_incomplete2, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.ADMIN, "/l1", json_incomplete3, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.ADMIN, "/l1", json_incomplete4, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.ADMIN, "/l1", json_incomplete5, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.ADMIN, "/l1", json_incomplete6, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.ADMIN, "/l1", json_incomplete7, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.ADMIN, "/l1", json_incomplete8, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.ADMIN, "/l1", json_incomplete9, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.ADMIN, "/l1", json_logbook_l1_name_na, HttpURLConnection.HTTP_BAD_REQUEST);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilLogbooks.assertListLogbooks(2,
                    default_logbooks[1],
                    default_logbooks[0]);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    void handleLogbookCreateCheck() {
        // what
        //     check(s) for create logbook
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 state      - null (empty, incorrect value (ok: Active, Inactive))

        Logbook logbook_check = new Logbook();

        ITUtilLogbooks.assertListLogbooks(2,
                default_logbooks[1],
                default_logbooks[0]);

        // check permissions
        // ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.USER,  "/l1", logbook_l1_owner_a_state_a, HttpURLConnection.HTTP_UNAUTHORIZED);

        ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.USER,  "/asdf", logbook_check, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.ADMIN, "/asdf", logbook_check, HttpURLConnection.HTTP_BAD_REQUEST);

        logbook_check.setName(null);

        ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.ADMIN, "/asdf", logbook_check, HttpURLConnection.HTTP_BAD_REQUEST);

        logbook_check.setName("");

        ITUtilLogbooks.assertCreateLogbook(AuthorizationChoice.ADMIN, "/asdf", logbook_check, HttpURLConnection.HTTP_BAD_REQUEST);

        ITUtilLogbooks.assertListLogbooks(2,
                default_logbooks[1],
                default_logbooks[0]);
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    void handleLogbook() {
        // what
        //     user with required role
        //     create tag
        //         list, create, list/retrieve, remove (unauthorized), remove, retrieve/list

        try {
            ITUtilLogbooks.assertListLogbooks(2,
                    default_logbooks[1],
                    default_logbooks[0]);

            ITUtilLogbooks.assertCreateLogbook("/l1", logbook_l1_owner_a_state_a);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilLogbooks.assertListLogbooks(3,
                    default_logbooks[1],
                    logbook_l1_owner_a_state_a,
                    default_logbooks[0]);

            ITUtilLogbooks.assertRetrieveLogbook("/l1", logbook_l1_owner_a_state_a);

            // check permissions
            // ITUtilLogbooks.assertRemoveLogbook(AuthorizationChoice.USER, "/l1", HttpURLConnection.HTTP_UNAUTHORIZED);

            ITUtilLogbooks.assertRemoveLogbook("/l1");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilLogbooks.assertRetrieveLogbook("/l1", logbook_l1_owner_a_state_i);

            ITUtilLogbooks.assertListLogbooks(2,
                    default_logbooks[1],
                    default_logbooks[0]);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    void handleLogbook2() {
        // what
        //     create logbooks, one by one
        //         list, create (2), list/retrieve, remove, list/retrieve, remove, retrieve/list

        try {
            ITUtilLogbooks.assertListLogbooks(2,
                    default_logbooks[1],
                    default_logbooks[0]);

            ITUtilLogbooks.assertCreateLogbook("/l1", logbook_l1_owner_a_state_a);
            ITUtilLogbooks.assertCreateLogbook("/l2", logbook_l2_owner_a_state_a);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilLogbooks.assertListLogbooks(4,
                    default_logbooks[1],
                    logbook_l1_owner_a_state_a,
                    logbook_l2_owner_a_state_a,
                    default_logbooks[0]);

            ITUtilLogbooks.assertRetrieveLogbook("/l1", logbook_l1_owner_a_state_a);
            ITUtilLogbooks.assertRetrieveLogbook("/l2", logbook_l2_owner_a_state_a);

            ITUtilLogbooks.assertRemoveLogbook("/l1");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilLogbooks.assertListLogbooks(3,
                    default_logbooks[1],
                    logbook_l2_owner_a_state_a,
                    default_logbooks[0]);

            ITUtilLogbooks.assertRetrieveLogbook("/l1", logbook_l1_owner_a_state_i);

            ITUtilLogbooks.assertRemoveLogbook("/l2");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilLogbooks.assertRetrieveLogbook("/l2", logbook_l2_owner_a_state_i);

            ITUtilLogbooks.assertListLogbooks(2,
                    default_logbooks[1],
                    default_logbooks[0]);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    void handleLogbook3ChangeState() {
        // what
        //     replace logbook, change state
        //         list, create, list/retrieve, update, list/retrieve, remove, retrieve/list

        try {
            ITUtilLogbooks.assertListLogbooks(2,
                    default_logbooks[1],
                    default_logbooks[0]);

            ITUtilLogbooks.assertCreateLogbook("/l1", logbook_l1_owner_a_state_a);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilLogbooks.assertListLogbooks(3,
                    default_logbooks[1],
                    logbook_l1_owner_a_state_a,
                    default_logbooks[0]);

            ITUtilLogbooks.assertRetrieveLogbook("/l1", logbook_l1_owner_a_state_a);

            ITUtilLogbooks.assertCreateLogbook("/l1", logbook_l1_owner_a_state_i);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilLogbooks.assertListLogbooks(2,
                    default_logbooks[1],
                    default_logbooks[0]);

            ITUtilLogbooks.assertRetrieveLogbook("/l1", logbook_l1_owner_a_state_i);

            ITUtilLogbooks.assertRemoveLogbook("/l1");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilLogbooks.assertRetrieveLogbook("/l1", logbook_l1_owner_a_state_i);

            ITUtilLogbooks.assertListLogbooks(2,
                    default_logbooks[1],
                    default_logbooks[0]);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    void handleLogbooksCreateCheck() {
        // what
        //     check(s) for create logbooks
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 state      - null, (empty, incorrect value (ok: Active, Inactive))

        Logbook logbook_check = new Logbook();
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

        ITUtilLogbooks.assertListLogbooks(2,
                default_logbooks[1],
                default_logbooks[0]);

        ITUtilLogbooks.assertCreateLogbooks("", logbooks, HttpURLConnection.HTTP_BAD_REQUEST);

        logbook_check.setName(null);
        logbooks[10] = logbook_check;

        ITUtilLogbooks.assertCreateLogbooks("", logbooks, HttpURLConnection.HTTP_BAD_REQUEST);

        logbook_check.setName("");
        logbooks[10] = logbook_check;

        ITUtilLogbooks.assertCreateLogbooks("", logbooks, HttpURLConnection.HTTP_BAD_REQUEST);

        ITUtilLogbooks.assertListLogbooks(2,
                default_logbooks[1],
                default_logbooks[0]);
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOGBOOK_RESOURCE_URI}.
     */
    @Test
    void handleLogbooks() {
        // what
        //     create logbooks
        //         list, create (10), list/retrieve, delete (5), list/retrieve, delete (5), retrieve/list

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

        try {
            ITUtilLogbooks.assertListLogbooks(2,
                    default_logbooks[1],
                    default_logbooks[0]);

            ITUtilLogbooks.assertCreateLogbooks("", logbooks_active_inactive);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilLogbooks.assertListLogbooks(7,
                    default_logbooks[1],
                    logbook_l1_owner_a_state_a,
                    logbook_l2_owner_a_state_a,
                    logbook_l3_owner_a_state_a,
                    logbook_l4_owner_a_state_a,
                    logbook_l5_owner_a_state_a,
                    default_logbooks[0]);

            ITUtilLogbooks.assertRetrieveLogbook("/l1", logbook_l1_owner_a_state_a);
            ITUtilLogbooks.assertRetrieveLogbook("/l2", logbook_l2_owner_a_state_a);
            ITUtilLogbooks.assertRetrieveLogbook("/l3", logbook_l3_owner_a_state_a);
            ITUtilLogbooks.assertRetrieveLogbook("/l4", logbook_l4_owner_a_state_a);
            ITUtilLogbooks.assertRetrieveLogbook("/l5", logbook_l5_owner_a_state_a);
            ITUtilLogbooks.assertRetrieveLogbook("/l6", logbook_l6_owner_a_state_i);
            ITUtilLogbooks.assertRetrieveLogbook("/l7", logbook_l7_owner_a_state_i);
            ITUtilLogbooks.assertRetrieveLogbook("/l8", logbook_l8_owner_a_state_i);
            ITUtilLogbooks.assertRetrieveLogbook("/l9", logbook_l9_owner_a_state_i);
            ITUtilLogbooks.assertRetrieveLogbook("/l10", logbook_l10_owner_a_state_i);

            ITUtilLogbooks.assertRemoveLogbook("/l1");
            ITUtilLogbooks.assertRemoveLogbook("/l2");
            ITUtilLogbooks.assertRemoveLogbook("/l3");
            ITUtilLogbooks.assertRemoveLogbook("/l9");
            ITUtilLogbooks.assertRemoveLogbook("/l10");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilLogbooks.assertListLogbooks(4,
                    default_logbooks[1],
                    logbook_l4_owner_a_state_a,
                    logbook_l5_owner_a_state_a,
                    default_logbooks[0]);

            ITUtilLogbooks.assertRemoveLogbook("/l4");
            ITUtilLogbooks.assertRemoveLogbook("/l5");
            ITUtilLogbooks.assertRemoveLogbook("/l6");
            ITUtilLogbooks.assertRemoveLogbook("/l7");
            ITUtilLogbooks.assertRemoveLogbook("/l8");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilLogbooks.assertListLogbooks(2,
                    default_logbooks[1],
                    default_logbooks[0]);
        } catch (Exception e) {
            fail();
        }
    }

}
