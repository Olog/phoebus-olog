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
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.github.dockerjava.api.DockerClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Optional;

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
    //     OLOG API                                                 LogbooksResource
    //     --------------------                                     --------------------
    //     Retrieve a Log                      .../logs/<id>        (GET)         getLog(String)
    //     Retrieve attachment for Log         .../logs/attachments/{logId}/{attachmentName}
    //                                                              (GET)         findResources(String, String)
    //     List Logs / Query by Pattern        .../logs             (GET)         findAll()
    //     Create a Log                        .../logs             (PUT)         createLog(String, Log, Principal)
    //     Upload attachment                   .../logs/attachments/{logId}
    //                                                              (POST)        uploadAttachment(String, MultipartFile, String, String, String)
    //     Upload multiple attachments         .../logs/attachments-multi/{logId}
    //                                                              (POST)        uploadMultipleAttachments(String, MultipartFile[])
    //     ------------------------------------------------------------------------------------------------

    @Container
    public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

    @AfterAll
    public static void extractJacocoReport() {
        // extract jacoco report from container file system
        //     stop jvm to make data available

        if (!Boolean.FALSE.toString().equals(System.getProperty(ITUtil.JACOCO_SKIPITCOVERAGE))) {
            return;
        }

        Optional<ContainerState> container = ENVIRONMENT.getContainerByServiceName(ITUtil.OLOG);
        if (container.isPresent()) {
            ContainerState cs = container.get();
            DockerClient dc = cs.getDockerClient();
            dc.stopContainerCmd(cs.getContainerId()).exec();
            try {
                cs.copyFileFromContainer(ITUtil.JACOCO_EXEC_PATH, ITUtil.JACOCO_TARGET_PREFIX + OlogLogsIT.class.getSimpleName() + ITUtil.JACOCO_TARGET_SUFFIX);
            } catch (Exception e) {
                // proceed if file cannot be copied
            }
        }
    }

    @Test
    void ologUp() {
        try {
            String address = ITUtil.HTTP_IP_PORT_OLOG;
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
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
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Log
        //         Retrieve attachment for Log
        //         List Logs / Query by Pattern
        //         Create a Log
        //         Upload attachment
        //         Upload multiple attachments

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
        //     --------------------------------------------------------------------------------
        //         Retrieve a Log
        //         Retrieve attachment for Log
        //     x   List Logs / Query by Pattern
        //     x   Create a Log
        //         Upload attachment
        //         Upload multiple attachments

        String json_incomplete1 = "{\"incomplete\"}";
        String json_incomplete2 = "{\"incomplete\"";
        String json_incomplete3 = "{\"incomplete}";
        String json_incomplete4 = "{\"\"}";
        String json_incomplete5 = "{incomplete\"}";
        String json_incomplete6 = "\"incomplete\"}";
        String json_incomplete7 = "{";
        String json_incomplete8 = "}";
        String json_incomplete9 = "\"";

        ITUtilLogs.assertListLogs(0);

        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete1, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete2, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete3, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete4, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete5, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete6, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete7, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete8, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", json_incomplete9, HttpURLConnection.HTTP_BAD_REQUEST);

        ITUtilLogs.assertListLogs(0);
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
        //     --------------------------------------------------------------------------------
        //         Retrieve a Log
        //         Retrieve attachment for Log
        //     x   List Logs / Query by Pattern
        //     x   Create a Log
        //         Upload attachment
        //         Upload multiple attachments

    	Log log_check = new Log.LogBuilder().build();

    	ITUtilLogs.assertListLogs(0);

    	ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", log_check, HttpURLConnection.HTTP_BAD_REQUEST);
    	ITUtilLogs.assertCreateLog(AuthorizationChoice.ADMIN, "", log_check, HttpURLConnection.HTTP_BAD_REQUEST);

    	ITUtilLogs.assertListLogs(0);
    }

}
