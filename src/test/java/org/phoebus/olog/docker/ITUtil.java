/*
 * Copyright (C) 2021 European Spallation Source ERIC.
 */

package org.phoebus.olog.docker;

import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.Tag;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Utility class to help (Docker) integration tests for Olog and Elasticsearch.
 *
 * @author Lars Johansson
 */
public class ITUtil {

    static final String OLOG  = "olog";

    static final String UTF_8 = "UTF-8";

    static final String AUTH_USER   = "user:userPass";
    static final String AUTH_ADMIN  = "admin:adminPass";
    static final String EMPTY_JSON  = "[]";
    static final String HEADER_JSON = "'Content-Type: application/json'";
    static final String HTTP        = "http://";

    static final String IP_PORT_OLOG          = "127.0.0.1:8080/Olog";
    static final String IP_PORT_ELASTICSEARCH = "127.0.0.1:9200";

    static final String HTTP_IP_PORT_OLOG          = HTTP + IP_PORT_OLOG;
    static final String HTTP_IP_PORT_ELASTICSEARCH = HTTP + IP_PORT_ELASTICSEARCH;

    private static final String BRACKET_BEGIN     = "[";
    private static final String BRACKET_END       = "]";
    private static final String CURLY_BRACE_BEGIN = "{";
    private static final String CURLY_BRACE_END   = "}";
    private static final String HTTP_REPLY        = "HTTP";

    // integration test - docker

    public static final String INTEGRATIONTEST_DOCKER_COMPOSE = "docker-compose-integrationtest.yml";
    public static final String INTEGRATIONTEST_LOG_MESSAGE    = ".*Started Application.*";

    /**
     * This class is not to be instantiated.
     */
    private ITUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Provide a default compose setup for testing.
     * For Docker Compose V2.
     *
     * Intended usage is as field annotated with @Container from class annotated with @Testcontainers.
     *
     * @return compose container
     */
    public static ComposeContainer defaultComposeContainers() {
        return new ComposeContainer(new File(ITUtil.INTEGRATIONTEST_DOCKER_COMPOSE))
                .withLocalCompose(true)
                .waitingFor(ITUtil.OLOG, Wait.forLogMessage(ITUtil.INTEGRATIONTEST_LOG_MESSAGE, 1));
    }

    /**
     * Refresh Elastic indices and return response code and string.
     *
     * @return response code and string
     * @throws IOException
     */
    static String[] refreshElasticIndices() throws IOException {
        return doGetJson(HTTP_IP_PORT_ELASTICSEARCH + "/_refresh");
    }

    /**
     * Do GET request with given string as URL and return response code.
     *
     * @param spec string to parse as URL
     * @return response code
     *
     * @throws IOException
     */
    static int doGet(String spec) throws IOException {
        URL url = new URL(spec);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        return con.getResponseCode();
    }

    /**
     * Do GET request with given string as URL and return response with string array with response code and response string.
     *
     * @param spec string to parse as URL
     * @return string array with response code and response string
     *
     * @throws IOException
     */
    static String[] doGetJson(String spec) throws IOException {
        URL url = new URL(spec);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int responseCode = con.getResponseCode();

        String line;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = responseCode == HttpURLConnection.HTTP_OK
                ? new BufferedReader(new InputStreamReader(con.getInputStream()))
                : new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
            while((line = br.readLine()) != null) {
                sb.append(line);
            }
        }

        return new String[] {String.valueOf(responseCode), sb.toString().trim()};
    }

    /**
     * Run a shell command and return response with string array with response code and response string.
     *
     * @param command shell command
     * @return string array with response code and response string
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws Exception
     */
    static String[] runShellCommand(String command) throws IOException, InterruptedException, Exception {
        // run shell command & return http response code if available

        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);

        String responseCode = null;
        String responseContent = null;
        try {
            final Process process = processBuilder.start();
            final BufferedReader errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            final BufferedReader inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
            final boolean processFinished = process.waitFor(30, TimeUnit.SECONDS);

            String line = null;
            while ((line = inputStream.readLine()) != null) {
                if (line.startsWith(HTTP_REPLY)) {
                    // response code, e.g. "HTTP/1.1 200", "HTTP/1.1 401", "HTTP/1.1 500"
                    String[] s = line.trim().split(" ");
                    if (s != null && s.length == 2) {
                        responseCode = s[1];
                    }
                } else if ((line.startsWith(BRACKET_BEGIN) && line.endsWith(BRACKET_END))
                        || (line.startsWith(CURLY_BRACE_BEGIN) && line.endsWith(CURLY_BRACE_END))) {
                    // response string, json
                    responseContent = line;
                }
            }

            if (!processFinished) {
                throw new Exception("Timed out waiting to execute command: " + command);
            }
            if (process.exitValue() != 0) {
                throw new Exception(
                        String.format("Shell command finished with status %d error: %s",
                                process.exitValue(),
                                errorStream.lines().collect(Collectors.joining())));
            }
        } catch (IOException | InterruptedException e) {
            throw e;
        }
        return new String[] {responseCode, responseContent};
    }

    /**
     * Assert that response object is as expected, an array with 2 elements
     * of which first contains response code OK (200).
     *
     * @param response string array with response of http request, response code and content
     *
     * @see HttpURLConnection#HTTP_OK
     */
    static void assertResponseLength2CodeOK(String[] response) {
        assertResponseLength2Code(response, HttpURLConnection.HTTP_OK);
    }

    /**
     * Assert that response object is as expected, an array with 2 elements
     * of which first element contains given response code.
     *
     * @param response string array with response of http request, response code and content
     * @param responseCode expected response code
     *
     * @see HttpURLConnection for available response codes
     */
    static void assertResponseLength2Code(String[] response, int responseCode) {
        assertNotNull(response);
        assertEquals(2, response.length);
        assertEquals(responseCode, Integer.parseInt(response[0]));
    }

    /**
     * Assert that response object is as expected, an array with 2 elements
     * of which first element contains response code OK (200) and second element contains given response content.
     *
     * @param response string array with response of http request, response code and content
     * @param responseContent expected response content
     *
     * @see HttpURLConnection#HTTP_OK
     */
    static void assertResponseLength2CodeOKContent(String[] response, String responseContent) {
        assertResponseLength2CodeContent(response, HttpURLConnection.HTTP_OK, responseContent);
    }

    /**
     * Assert that response object is as expected, an array with 2 elements
     * of which first element contains given response code and second element contains given response content.
     *
     * @param response string array with response of http request, response code and content
     * @param responseCode expected response code
     * @param responseContent expected response content
     *
     * @see HttpURLConnection for available response codes
     */
    static void assertResponseLength2CodeContent(String[] response, int responseCode, String responseContent) {
        assertResponseLength2Code(response, responseCode);
        assertEquals(responseContent, response[1]);
    }

    /**
     * Assert that arrays are equal with same length and same content in each array position.
     *
     * @param actual actual array of Tag objects
     * @param expected expected arbitrary number of Tag objects
     */
    static void assertEqualsTags(Tag[] actual, Tag... expected) {
        if (expected != null) {
            assertNotNull(actual);
            assertEquals(expected.length, actual.length);
            for (int i=0; i<expected.length; i++) {
                assertEquals(expected[i], actual[i]);
            }
        } else {
            assertNull(actual);
        }
    }

    /**
     * Assert that arrays are equal with same length and same content in each array position.
     *
     * @param actual actual array of Logbook objects
     * @param expected expected arbitrary number of Logbook objects
     */
    static void assertEqualsLogbooks(Logbook[] actual, Logbook... expected) {
        if (expected != null) {
            assertNotNull(actual);
            assertEquals(expected.length, actual.length);
            for (int i=0; i<expected.length; i++) {
                assertEquals(expected[i], actual[i]);
            }
        } else {
            assertNull(actual);
        }
    }

    /**
     * Assert that arrays are equal with same length and same content in each array position.
     *
     * @param actual actual array of Property objects
     * @param expected expected arbitrary number of Property objects
     */
    static void assertEqualsProperties(Property[] actual, Property... expected) {
        if (expected != null) {
            assertNotNull(actual);
            assertEquals(expected.length, actual.length);
            for (int i=0; i<expected.length; i++) {
                assertEquals(expected[i], actual[i]);
            }
        } else {
            assertNull(actual);
        }
    }

    /**
     * Assert that arrays are equal with same length and same content in each array position.
     *
     * @param actual actual array of Log objects
     * @param expected expected arbitrary number of Log objects
     */
    static void assertEqualsLogs(Log[] actual, Log... expected) {
        if (expected != null) {
            assertNotNull(actual);
            assertEquals(expected.length, actual.length);
            for (int i=0; i<expected.length; i++) {
                assertEquals(expected[i], actual[i]);
            }
        } else {
            assertNull(actual);
        }
    }


}
