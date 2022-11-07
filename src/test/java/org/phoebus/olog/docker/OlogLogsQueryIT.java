/*
 * Copyright (C) 2021 European Spallation Source ERIC.
 */

package org.phoebus.olog.docker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.phoebus.olog.entity.Log;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
public class OlogLogsQueryIT {

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
    //         see test QueryByPattern for list of logs which match given expressions
    //     ------------------------------------------------------------------------------------------------
    //     Olog - Service Documentation
    //         https://olog.readthedocs.io/en/latest/
    //     ------------------------------------------------------------------------------------------------
    //     OLOG API                                         LogbooksResource
    //     --------------------                             --------------------
    //     Retrieve a Log                  .../logs/<id>    (GET)       getLog(String)
    //     Retrieve attachment for Log     .../logs/attachments/{logId}/{attachmentName}
    //                                                      (GET)       findResources(String, String)
    //     List Logs / Query by Pattern    .../logs         (GET)       findAll()
    //     Create a Log                    .../logs         (PUT)       createLog(String, Log, Principal)
    //     Upload attachment               .../logs/attachments/{logId}
    //                                                      (POST)      uploadAttachment(String, MultipartFile, String, String, String)
    //     Upload multiple attachments     .../logs/attachments-multi/{logId}
    //                                                      (POST)      uploadMultipleAttachments(String, MultipartFile[])
    //     ------------------------------------------------------------------------------------------------

    static final String LOGS = "/logs";

    static final String HTTP_IP_PORT_OLOG_LOGS            = ITUtil.HTTP +                           ITUtil.IP_PORT_OLOG + LOGS;
    static final String HTTP_AUTH_USER_IP_PORT_OLOG_LOGS  = ITUtil.HTTP + ITUtil.AUTH_USER  + "@" + ITUtil.IP_PORT_OLOG + LOGS;
    static final String HTTP_AUTH_ADMIN_IP_PORT_OLOG_LOGS = ITUtil.HTTP + ITUtil.AUTH_ADMIN + "@" + ITUtil.IP_PORT_OLOG + LOGS;

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

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOG_RESOURCE_URI}.
     */
    @Test
    public void handleLogsQueryByPattern() {
        // what
        //     query by pattern
        //     --------------------------------------------------------------------------------
        //     set up test fixture
        //     test
        //         query by pattern
        //             combine search parameters and logs (logbooks, tags, properties, attachments)
        //     tear down test fixture

        // --------------------------------------------------------------------------------
        // set up test fixture
        // --------------------------------------------------------------------------------

        ITTestFixture.setup();

        // --------------------------------------------------------------------------------
        // query by pattern
        //     --------------------------------------------------------------------------------
        //     search parameters
        //         keyword
        //             text
        //                 owner
        //                 desc
        //                 description
        //                 title
        //                 level
        //                 phrase        (description)
        //                 fuzzy         (?)
        //             meta data
        //                 logbooks
        //                 tags
        //                 properties
        //             time
        //                 start         (createdDate)
        //                 end           (createdDate)
        //                 includeevent  (with start, end)
        //                 includeevents (with start, end)
        //             default
        //                 unsupported search parameters are ignored
        //     --------------------------------------------------------------------------------
        //     query for pattern
        //         existing
        //         non-existing
        //         exact
        //         not exact
        //         combinations
        //     --------------------------------------------------------------------------------
        //     search for
        //         existing
        //         non-existing
        //     --------------------------------------------------------------------------------

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS);
            ITUtil.assertResponseLength2CodeOK(response);
            Log[] logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            // keyword
            // text
            // owner
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?owner");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?owner=asdf");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?owner=admin");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?owner=adm?n");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?owner=adm?m");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?owner=adm*");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            // desc
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc=asdf");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc=Initial");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc=check");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(8, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc=Check");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(8, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc=complete");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(4, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc=Complete");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(4, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc=check complete");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_VERSION);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc='check complete'");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_VERSION);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc=check&desc=complete");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(10, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc="+URLEncoder.encode("check complete", StandardCharsets.UTF_8));
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(10, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc='"+URLEncoder.encode("check complete", StandardCharsets.UTF_8)+"'");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc="+URLEncoder.encode("CHECK COMPLETE", StandardCharsets.UTF_8));
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(10, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc='"+URLEncoder.encode("CHECK COMPLETE", StandardCharsets.UTF_8)+"'");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc=chec?");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(8, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc=?omplete");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(4, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?desc=c*");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(18, logs.length);

            // description
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description=asdf");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description=Initial");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description=check");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(8, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description=Check");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(8, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description=complete");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(4, logs.length);
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description=Complete");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(4, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description=check complete");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_VERSION);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description='check complete'");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_VERSION);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description=check&desc=complete");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(10, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description="+URLEncoder.encode("check complete", StandardCharsets.UTF_8));
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(10, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description='"+URLEncoder.encode("check complete", StandardCharsets.UTF_8)+"'");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description="+URLEncoder.encode("CHECK COMPLETE", StandardCharsets.UTF_8));
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(10, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description='"+URLEncoder.encode("CHECK COMPLETE", StandardCharsets.UTF_8)+"'");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description=chec?");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(8, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description=?omplete");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(4, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?description=c*");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(18, logs.length);

            // title
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title=asdf");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title=shift");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(43, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title=Shift");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(43, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title=update");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(37, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title=Update");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(37, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title=shift update");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_VERSION);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title='shift update'");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_VERSION);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title=shift&title=update");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(43, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title="+URLEncoder.encode("shift update", StandardCharsets.UTF_8));
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(43, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title='"+URLEncoder.encode("shift update", StandardCharsets.UTF_8)+"'");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title="+URLEncoder.encode("SHIFT UPDATE", StandardCharsets.UTF_8));
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(43, logs.length);
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title='"+URLEncoder.encode("SHIFT UPDATE", StandardCharsets.UTF_8)+"'");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title=Shif?");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(43, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title=??ift");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(43, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?title=S*t");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(43, logs.length);

            // level
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level=asdf");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level=shift");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level=Shift");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level=update");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(54, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level=Update");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(54, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level=shift update");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_VERSION);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level='shift update'");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_VERSION);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level=shift&level=update");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level="+URLEncoder.encode("shift update", StandardCharsets.UTF_8));
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level='"+URLEncoder.encode("shift update", StandardCharsets.UTF_8)+"'");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level="+URLEncoder.encode("SHIFT UPDATE", StandardCharsets.UTF_8));
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level='"+URLEncoder.encode("SHIFT UPDATE", StandardCharsets.UTF_8)+"'");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level=?pdate");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(54, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level=upd??e");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(54, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?level=*ate");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(54, logs.length);

            // phrase
            //     phrase for description
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?phrase");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?phrase=asdf");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?phrase=check");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(8, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?phrase=Check");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(8, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?phrase=complete");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(4, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?phrase=Complete");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(4, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?phrase=check complete");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_VERSION);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?phrase='check complete'");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_VERSION);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?phrase=check&phrase=complete");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(10, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?phrase="+URLEncoder.encode("check complete", StandardCharsets.UTF_8));
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(2, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?phrase='"+URLEncoder.encode("check complete", StandardCharsets.UTF_8)+"'");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(2, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?phrase="+URLEncoder.encode("CHECK COMPLETE", StandardCharsets.UTF_8));
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(2, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?phrase='"+URLEncoder.encode("CHECK COMPLETE", StandardCharsets.UTF_8)+"'");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(2, logs.length);

            // fuzzy
            //     fuzziness AUTO
            //     description
            //     title
            //     level
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?fuzzy");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?fuzzy&description=cmplete");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(4, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?fuzzy&description=cmplte");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(4, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?fuzzy&title=Shif");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(43, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?fuzzy&title=Shif?");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(43, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?fuzzy&title=Shif*");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(43, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?fuzzy&title=Shi??");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?fuzzy&title=hif");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?fuzzy&level=pdate");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(54, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?fuzzy&level=Upd");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            // meta data
            // logbooks
            //     name
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=asdf");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=Buildings");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=Communication");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=Experiments");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=Facilities");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=Maintenance");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(17, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=operations");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=Operations");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(49, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=operation");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=Power");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(2, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=Services");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=Water");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=Maintenance&logbooks=Power");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(18, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=Maint*");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(17, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=?e?");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=*e*");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=x*x");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            // tags
            //     name
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=asdf");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=cryo");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=Cryo");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(10, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=Cry");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=Power");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(2, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=Safety");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(2, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=Source");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(2, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=Initial");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(2, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=Radio");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(2, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=Magnet");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(2, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=Supra");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(3, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=Magnet&tags=Supra");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(5, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=?ryo");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(10, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=*yo");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(10, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=C???");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(10, logs.length);

            // properties
            //     name
            //     attribute name
            //     attribute value
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=asdf");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=a");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=A");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(20, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=B");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=C");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=Shift Info C");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_VERSION);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties='Shift Info C'");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_VERSION);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties="+URLEncoder.encode("Shift Info C", StandardCharsets.UTF_8));
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(20, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties='"+URLEncoder.encode("Shift Info C", StandardCharsets.UTF_8)+"'");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=.operator");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=.Operator");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=..12345678c");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=..12345678C");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(20, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=..*C");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(20, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=..12345678?");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=..12345*");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            // time
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?start");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?start=asdf");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?end");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?end=asdf");
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            // default
            //     unsupported search parameters are ignored
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?zxcv");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?zxcv=asdf");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(60, logs.length);

            // combinations
            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?logbooks=*&description=maintenance");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(2, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?tags=*&description=maintenance");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(1, logs.length);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=..12345678A&phrase="+URLEncoder.encode("Start-up after maintenance", StandardCharsets.UTF_8));
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=..12345678C&phrase="+URLEncoder.encode("Start-up after maintenance", StandardCharsets.UTF_8));
            // expected 3
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=..123*&phrase="+URLEncoder.encode("Start-up after maintenance", StandardCharsets.UTF_8));
            // expected 3
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.doGetJson(OlogLogsQueryIT.HTTP_IP_PORT_OLOG_LOGS + "?properties=..123*&description=maintenance");
            ITUtil.assertResponseLength2CodeOK(response);
            logs = mapper.readValue(response[1], Log[].class);
            assertNotNull(logs);
            assertEquals(2, logs.length);
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }

        // --------------------------------------------------------------------------------
        // tear down test fixture
        // --------------------------------------------------------------------------------

        ITTestFixture.tearDown();
    }

}
