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
import org.junit.jupiter.api.Test;
import org.phoebus.olog.docker.ITUtil.AuthorizationChoice;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.State;
import org.phoebus.olog.entity.Log.LogBuilder;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests for Olog and Elasticsearch that make use of existing dockerization
 * with docker-compose.yml / Dockerfile.
 *
 * <p>
 * Focus of this class is to have Olog and Elasticsearch up and running together with usage of
 * {@link org.phoebus.olog.OlogResourceDescriptors#LOG_RESOURCE_URI}.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.olog.LogResource
 */
@Testcontainers
class OlogLogsIT {

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
    //     Retrieve a Log                      .../logs/<id>                                        GET
    //     Retrieve attachment for Log         .../logs/attachments/{logId}/{attachmentName}        GET
    //     List Logs / Query by Pattern        .../logs                                             GET
    //     Create a Log                        .../logs                                             PUT
    //     Upload attachment                   .../logs/attachments/{logId}                         POST
    //     Upload multiple attachments         .../logs/attachments-multi/{logId}                   POST
    //     ------------------------------------------------------------------------------------------------

    @Container
    public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

    @AfterAll
    public static void extractJacocoReport() {
        // extract jacoco report from container file system
        ITUtil.extractJacocoReport(ENVIRONMENT,
                ITUtil.JACOCO_TARGET_PREFIX + OlogLogsIT.class.getSimpleName() + ITUtil.JACOCO_TARGET_SUFFIX);
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
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOG_RESOURCE_URI}.
     */
    @Test
    void handleLogRetrieveCheck() {
        // what
        //     check(s) for retrieve log
        //         e.g.
        //             retrieve non-existing log

        ITUtilTags.assertRetrieveTag("/l11", HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOG_RESOURCE_URI}.
     */
    @Test
    void handleLogCreateCheckJson() {
        // what
        //     check(s) for create log
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

        int length = ITUtilLogs.assertListLogs(-1).length;

        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete1, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete2, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete3, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete4, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete5, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete6, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete7, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete8, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete9, HttpURLConnection.HTTP_BAD_REQUEST);

        assertEquals(length, ITUtilLogs.assertListLogs(-1).length);
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOG_RESOURCE_URI}.
     */
    @Test
    void handleLogCreateCheck() {
        // what
        //     check(s) for create log
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 state      - null (empty, incorrect value (ok: Active, Inactive))

        Log log_check = new Log.LogBuilder().build();

        int length = ITUtilLogs.assertListLogs(-1).length;

        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", log_check, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", log_check, HttpURLConnection.HTTP_BAD_REQUEST);

        assertEquals(length, ITUtilLogs.assertListLogs(-1).length);
    }

    @Test
    void handleLogGroup() {
    	// create logbook and prepare logs
    	Logbook logbook1 = new Logbook("name1", "user", State.Active);
    	ITUtilLogbooks.assertCreateLogbook("/" + logbook1.getName(), logbook1);

    	Log log1 = LogBuilder.createLog()
    			.owner("owner")
    			.title("title1")
    			.withLogbooks(Set.of(logbook1))
    			.build();
    	Log log2 = LogBuilder.createLog()
    			.owner("user")
    			.title("title2")
    			.withLogbooks(Set.of(logbook1))
    			.build();

    	// create logs
    	log1 = ITUtilLogs.assertCreateLog("", log1);
    	log2 = ITUtilLogs.assertCreateLog("", log2);
    	assertEquals(0, log1.getProperties().size());
    	assertEquals(0, log2.getProperties().size());

    	// group logs
    	ITUtilLogs.assertGroupLogs(Arrays.asList(log1.getId(), log2.getId()));
    	log1 = ITUtilLogs.assertRetrieveLog("/" + log1.getId());
    	log2 = ITUtilLogs.assertRetrieveLog("/" + log2.getId());
    	assertEquals(1, log1.getProperties().size());
    	assertEquals(1, log2.getProperties().size());

    	// edit and update logs but log group to remain
    	log1.getProperties().clear();
    	log1 = ITUtilLogs.assertUpdateLog(log1);
    	log2 = ITUtilLogs.assertRetrieveLog("/" + log2.getId());
    	assertEquals(1, log1.getProperties().size());
    	assertEquals(1, log2.getProperties().size());
    }

}
