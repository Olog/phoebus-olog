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
 * Focus of this class is to have Olog and Elasticsearch up and running.
 *
 * @author Lars Johansson
 */
@Testcontainers
class OlogIT {

    // Note
    //     ------------------------------------------------------------------------------------------------
    //     About
    //         requires
    //             elastic indices for Olog, ensured at start-up
    //             environment
    //                 default ports, 8080 for Olog, 9200 for Elasticsearch
    //                 demo_auth enabled
    //         docker containers shared for tests
    //             each test to leave Olog, Elasticsearch in clean state - not disturb other tests
    //         each test uses multiple endpoints in Olog API
    //     ------------------------------------------------------------------------------------------------
    //     Olog - Service Documentation
    //         https://olog.readthedocs.io/en/latest/
    //     ------------------------------------------------------------------------------------------------

    @Container
    public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

    @AfterAll
    public static void extractJacocoReport() {
        // extract jacoco report from container file system
        ITUtil.extractJacocoReport(ENVIRONMENT,
                ITUtil.JACOCO_TARGET_PREFIX + OlogIT.class.getSimpleName() + ITUtil.JACOCO_TARGET_SUFFIX);
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
    @Test
    void ologUpTags() {
        try {
            int responseCode = ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_OLOG + "/tags");

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (Exception e) {
            fail();
        }
    }
    @Test
    void ologUpLogbooks() {
        try {
            int responseCode = ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_OLOG + "/logbooks");

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (Exception e) {
            fail();
        }
    }
    @Test
    void ologUpProperties() {
        try {
            int responseCode = ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_OLOG + "/properties");

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (Exception e) {
            fail();
        }
    }
    @Test
    void ologUpLogs() {
        try {
            int responseCode = ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_OLOG + "/logs");

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (Exception e) {
            fail();
        }
    }
    @Test
    void ologUpConfiguration() {
        try {
            int responseCode = ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_OLOG + "/configuration");

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (Exception e) {
            fail();
        }
    }
    @Test
    void ologUpAttachment() {
        try {
            int responseCode = ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_OLOG + "/attachment");

            assertEquals(HttpURLConnection.HTTP_NOT_FOUND, responseCode);
        } catch (Exception e) {
            fail();
        }
    }

}
