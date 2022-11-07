/*
 * Copyright (C) 2021 European Spallation Source ERIC.
 */

package org.phoebus.olog.docker;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
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
public class OlogIT {

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
    public static final DockerComposeContainer<?> ENVIRONMENT =
        new DockerComposeContainer<>(new File("docker-compose.yml"))
            .waitingFor(ITUtil.OLOG, Wait.forLogMessage(".*Started Application.*", 1));

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
    @Test
    public void ologUpTags() {
        try {
            String address = ITUtil.HTTP_IP_PORT_OLOG + "/tags";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    public void ologUpLogbooks() {
        try {
            String address = ITUtil.HTTP_IP_PORT_OLOG + "/logbooks";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    public void ologUpProperties() {
        try {
            String address = ITUtil.HTTP_IP_PORT_OLOG + "/properties";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    public void ologUpLogs() {
        try {
            String address = ITUtil.HTTP_IP_PORT_OLOG + "/logs";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    public void ologUpConfiguration() {
        try {
            String address = ITUtil.HTTP_IP_PORT_OLOG + "/configuration";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    public void ologUpAttachment() {
        try {
            String address = ITUtil.HTTP_IP_PORT_OLOG + "/attachment";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_NOT_FOUND, responseCode);
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void elasticsearchUp() {
        try {
            String address = ITUtil.HTTP_IP_PORT_ELASTICSEARCH;
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    public void elasticsearchUpHealthcheck() {
        try {
            String address = ITUtil.HTTP_IP_PORT_ELASTICSEARCH + "/_cat/health";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    public void elasticsearchUpTags() {
        try {
            String address = ITUtil.HTTP_IP_PORT_ELASTICSEARCH + "/olog_tags";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    public void elasticsearchUpLogbooks() {
        try {
            String address = ITUtil.HTTP_IP_PORT_ELASTICSEARCH + "/olog_logbooks";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    public void elasticsearchUpProperties() {
        try {
            String address = ITUtil.HTTP_IP_PORT_ELASTICSEARCH + "/olog_properties";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    public void elasticsearchUpSequence() {
        try {
            String address = ITUtil.HTTP_IP_PORT_ELASTICSEARCH + "/olog_sequence";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_NOT_FOUND, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    public void elasticsearchUpLogs() {
        try {
            String address = ITUtil.HTTP_IP_PORT_ELASTICSEARCH + "/olog_logs";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_NOT_FOUND, responseCode);
        } catch (IOException e) {
            fail();
        }
    }

}
