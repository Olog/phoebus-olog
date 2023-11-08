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

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
class OlogLogsQueryIT {

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
    void handleLogsQueryByPattern() {
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
        //             desc, description, text
        //             title
        //             fuzzy
        //             phrase
        //             owner
        //             tags
        //             logbooks
        //             start
        //             end
        //             includeevents, includeevent
        //             properties
        //             level
        //             size, limit
        //             from
        //             sort
        //             attachments
        //         keyword combinations
        //     --------------------------------------------------------------------------------
        //     default
        //         unsupported search parameters are ignored
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

        try {
            ITUtilLogs.assertListLogs(60);

            // ----------------------------------------------------------------------------------------------------
            // keyword
            // ----------------------------------------------------------------------------------------------------

            // desc, description, text
            ITUtilLogs.assertListLogs("?desc", 60);
            ITUtilLogs.assertListLogs("?desc=asdf", 0);
            ITUtilLogs.assertListLogs("?desc=Initial", 2);
            ITUtilLogs.assertListLogs("?desc=check", 0);
            ITUtilLogs.assertListLogs("?desc=Check", 0);
            ITUtilLogs.assertListLogs("?desc=complete", 0);
            ITUtilLogs.assertListLogs("?desc=Complete", 0);
            ITUtilLogs.assertListLogs("?desc=check complete", HttpURLConnection.HTTP_BAD_REQUEST, -1, -1);
            ITUtilLogs.assertListLogs("?desc='check complete'", HttpURLConnection.HTTP_BAD_REQUEST, -1, -1);
            ITUtilLogs.assertListLogs("?desc=Maintenance", 17);
            ITUtilLogs.assertListLogs("?desc=maintenance", 17);
            ITUtilLogs.assertListLogs("?desc=after", 3);
            ITUtilLogs.assertListLogs("?desc=Maintenance&desc=after", 3);
            ITUtilLogs.assertListLogs("?desc=maintenance&desc=after", 3);
            ITUtilLogs.assertListLogs("?desc=after&desc=maintenance", 3);
            ITUtilLogs.assertListLogs("?desc=after&desc=Maintenance", 3);
            ITUtilLogs.assertListLogs("?desc="+URLEncoder.encode("after maintenance", StandardCharsets.UTF_8), 3);
            ITUtilLogs.assertListLogs("?desc='"+URLEncoder.encode("after maintenance", StandardCharsets.UTF_8)+"'", 0);
            ITUtilLogs.assertListLogs("?desc=\""+URLEncoder.encode("after maintenance", StandardCharsets.UTF_8)+"\"", HttpURLConnection.HTTP_BAD_REQUEST, -1, -1);
            ITUtilLogs.assertListLogs("?desc=check&desc=complete", 0);
            ITUtilLogs.assertListLogs("?desc="+URLEncoder.encode("check complete", StandardCharsets.UTF_8), 0);
            ITUtilLogs.assertListLogs("?desc='"+URLEncoder.encode("check complete", StandardCharsets.UTF_8)+"'", 0);
            ITUtilLogs.assertListLogs("?desc="+URLEncoder.encode("CHECK COMPLETE", StandardCharsets.UTF_8), 0);
            ITUtilLogs.assertListLogs("?desc='"+URLEncoder.encode("CHECK COMPLETE", StandardCharsets.UTF_8)+"'", 0);
            ITUtilLogs.assertListLogs("?desc=chec?", 0);
            ITUtilLogs.assertListLogs("?desc=?omplete", 0);
            ITUtilLogs.assertListLogs("?desc=c*", 9);

            // ----------------------------------------------------------------------------------------------------

            // title
            ITUtilLogs.assertListLogs("?title", 60);
            ITUtilLogs.assertListLogs("?title=asdf", 0);
            ITUtilLogs.assertListLogs("?title=shift", 43);
            ITUtilLogs.assertListLogs("?title=Shift", 43);
            ITUtilLogs.assertListLogs("?title=update", 37);
            ITUtilLogs.assertListLogs("?title=Update", 37);
            ITUtilLogs.assertListLogs("?title=shift update", HttpURLConnection.HTTP_BAD_REQUEST, -1, -1);
            ITUtilLogs.assertListLogs("?title='shift update'", HttpURLConnection.HTTP_BAD_REQUEST, -1, -1);
            ITUtilLogs.assertListLogs("?title=shift&title=update", 37);
            ITUtilLogs.assertListLogs("?title="+URLEncoder.encode("shift update", StandardCharsets.UTF_8), 37);
            ITUtilLogs.assertListLogs("?title='"+URLEncoder.encode("shift update", StandardCharsets.UTF_8)+"'", 0);
            ITUtilLogs.assertListLogs("?title="+URLEncoder.encode("SHIFT UPDATE", StandardCharsets.UTF_8), 37);
            ITUtilLogs.assertListLogs("?title='"+URLEncoder.encode("SHIFT UPDATE", StandardCharsets.UTF_8)+"'", 0);
            ITUtilLogs.assertListLogs("?title=Shif?", 43);
            ITUtilLogs.assertListLogs("?title=??ift", 43);
            ITUtilLogs.assertListLogs("?title=S*t", 43);

            // ----------------------------------------------------------------------------------------------------

            // fuzzy
            //     fuzziness AUTO
            //     description
            //     title
            //     level
            ITUtilLogs.assertListLogs("?fuzzy", 60);
            ITUtilLogs.assertListLogs("?fuzzy&description=cmplete", 0);
            ITUtilLogs.assertListLogs("?fuzzy&description=cmplte", 0);
            ITUtilLogs.assertListLogs("?fuzzy&title=Shif", 43);
            ITUtilLogs.assertListLogs("?fuzzy&title=Shif?", 43);
            ITUtilLogs.assertListLogs("?&title=Shif*", 43);
            ITUtilLogs.assertListLogs("?fuzzy&title=Shi??", 0);
            ITUtilLogs.assertListLogs("?fuzzy&title=hif", 0);
            ITUtilLogs.assertListLogs("?fuzzy&level=pdate", 54);
            ITUtilLogs.assertListLogs("?fuzzy&level=Upd", 0);

            // ----------------------------------------------------------------------------------------------------

            // phrase
            //     phrase for description
            ITUtilLogs.assertListLogs("?phrase", 0);
            ITUtilLogs.assertListLogs("?phrase=asdf", 0);
            ITUtilLogs.assertListLogs("?phrase=Initial", 2);
            ITUtilLogs.assertListLogs("?phrase=check", 0);
            ITUtilLogs.assertListLogs("?phrase=Check", 0);
            ITUtilLogs.assertListLogs("?phrase=complete", 0);
            ITUtilLogs.assertListLogs("?phrase=Complete", 0);
            ITUtilLogs.assertListLogs("?phrase=check complete", HttpURLConnection.HTTP_BAD_REQUEST, -1, -1);
            ITUtilLogs.assertListLogs("??phrase='check complete'", HttpURLConnection.HTTP_BAD_REQUEST, -1, -1);
            ITUtilLogs.assertListLogs("?phrase=check&phrase=complete", 0);
            ITUtilLogs.assertListLogs("?phrase="+URLEncoder.encode("check complete", StandardCharsets.UTF_8), 0);
            ITUtilLogs.assertListLogs("?phrase='"+URLEncoder.encode("check complete", StandardCharsets.UTF_8)+"'", 0);
            ITUtilLogs.assertListLogs("?phrase="+URLEncoder.encode("CHECK COMPLETE", StandardCharsets.UTF_8), 0);
            ITUtilLogs.assertListLogs("?phrase='"+URLEncoder.encode("CHECK COMPLETE", StandardCharsets.UTF_8)+"'", 0);

            // ----------------------------------------------------------------------------------------------------

            // owner
            ITUtilLogs.assertListLogs("?owner", 0);
            ITUtilLogs.assertListLogs("?owner=asdf", 0);
            ITUtilLogs.assertListLogs("?owner=admin", 60);
            ITUtilLogs.assertListLogs("?owner=adm?n", 60);
            ITUtilLogs.assertListLogs("?owner=adm?m", 0);
            ITUtilLogs.assertListLogs("?owner=adm*", 60);

            // ----------------------------------------------------------------------------------------------------

            // tags
            //     name
            ITUtilLogs.assertListLogs("?tags", 0);
            ITUtilLogs.assertListLogs("?tags=asdf", 0);
            ITUtilLogs.assertListLogs("?tags=cryo", 10);
            ITUtilLogs.assertListLogs("?tags=Cryo", 10);
            ITUtilLogs.assertListLogs("?tags=Cry", 0);
            ITUtilLogs.assertListLogs("?tags=Power", 2);
            ITUtilLogs.assertListLogs("?tags=Safety", 2);
            ITUtilLogs.assertListLogs("?tags=Source", 2);
            ITUtilLogs.assertListLogs("?tags=Initial", 2);
            ITUtilLogs.assertListLogs("?tags=Radio", 2);
            ITUtilLogs.assertListLogs("?tags=Magnet", 2);
            ITUtilLogs.assertListLogs("?tags=Supra", 3);
            ITUtilLogs.assertListLogs("?tags=Magnet&tags=Supra", 5);
            ITUtilLogs.assertListLogs("?tags=?ryo", 10);
            ITUtilLogs.assertListLogs("?tags=*yo", 10);
            ITUtilLogs.assertListLogs("?tags=C???", 10);

            // ----------------------------------------------------------------------------------------------------

            // logbooks
            //     name
            ITUtilLogs.assertListLogs("?logbooks", 0);
            ITUtilLogs.assertListLogs("?logbooks=asdf", 0);
            ITUtilLogs.assertListLogs("?logbooks=Buildings", 0);
            ITUtilLogs.assertListLogs("?logbooks=Communication", 0);
            ITUtilLogs.assertListLogs("?logbooks=Experiments", 0);
            ITUtilLogs.assertListLogs("?logbooks=Facilities", 0);
            ITUtilLogs.assertListLogs("?logbooks=Maintenance", 17);
            ITUtilLogs.assertListLogs("?logbooks=operations", 49);
            ITUtilLogs.assertListLogs("?logbooks=Operations", 49);
            ITUtilLogs.assertListLogs("?logbooks=operation", 0);
            ITUtilLogs.assertListLogs("?logbooks=Power", 2);
            ITUtilLogs.assertListLogs("?logbooks=Services", 0);
            ITUtilLogs.assertListLogs("?logbooks=Water", 0);
            ITUtilLogs.assertListLogs("?logbooks=Maintenance&logbooks=Power", 18);
            ITUtilLogs.assertListLogs("?logbooks=Maint*", 17);
            ITUtilLogs.assertListLogs("?logbooks=?e?", 0);
            ITUtilLogs.assertListLogs("?logbooks=*e*", 60);
            ITUtilLogs.assertListLogs("?logbooks=x*x", 0);

            // ----------------------------------------------------------------------------------------------------

            // start
            // end
            ITUtilLogs.assertListLogs("?start", 0);
            ITUtilLogs.assertListLogs("?start=asdf", 60);
            ITUtilLogs.assertListLogs("?end", 60);
            ITUtilLogs.assertListLogs("?end=asdf", 0);

            // ----------------------------------------------------------------------------------------------------

            // properties
            //     name
            //     attribute name
            //     attribute value
            ITUtilLogs.assertListLogs("?properties", 60);
            ITUtilLogs.assertListLogs("?properties=asdf", 0);
            ITUtilLogs.assertListLogs("?properties=a", 20);
            ITUtilLogs.assertListLogs("?properties=A", 20);
            ITUtilLogs.assertListLogs("?properties=B", 0);
            ITUtilLogs.assertListLogs("?properties=C", 0);
            ITUtilLogs.assertListLogs("?properties=Shift Info C", HttpURLConnection.HTTP_BAD_REQUEST, -1, -1);
            ITUtilLogs.assertListLogs("?properties='Shift Info C'", HttpURLConnection.HTTP_BAD_REQUEST, -1, -1);
            ITUtilLogs.assertListLogs("?properties="+URLEncoder.encode("Shift Info C", StandardCharsets.UTF_8), 20);
            ITUtilLogs.assertListLogs("?properties='"+URLEncoder.encode("Shift Info C", StandardCharsets.UTF_8)+"'", 0);
            ITUtilLogs.assertListLogs("?properties=.operator", 60);
            ITUtilLogs.assertListLogs("?properties=.Operator", 60);
            ITUtilLogs.assertListLogs("?properties=..12345678c", 60);
            ITUtilLogs.assertListLogs("?properties=..12345678C", 60);
            ITUtilLogs.assertListLogs("?properties=..*C", 60);
            ITUtilLogs.assertListLogs("?properties=..12345678?", 60);
            ITUtilLogs.assertListLogs("?properties=..12345*", 60);

            // ----------------------------------------------------------------------------------------------------

            // level
            ITUtilLogs.assertListLogs("?level", 0);
            ITUtilLogs.assertListLogs("?level=asdf", 0);
            ITUtilLogs.assertListLogs("?level=shift", 60);
            ITUtilLogs.assertListLogs("?level=Shift", 60);
            ITUtilLogs.assertListLogs("?level=update", 54);
            ITUtilLogs.assertListLogs("?level=Update", 54);
            ITUtilLogs.assertListLogs("?level=shift update", HttpURLConnection.HTTP_BAD_REQUEST, -1, -1);
            ITUtilLogs.assertListLogs("?level='shift update'", HttpURLConnection.HTTP_BAD_REQUEST, -1, -1);
            ITUtilLogs.assertListLogs("?level=shift&level=update", 60);
            ITUtilLogs.assertListLogs("?level="+URLEncoder.encode("shift update", StandardCharsets.UTF_8), 60);
            ITUtilLogs.assertListLogs("?level='"+URLEncoder.encode("shift update", StandardCharsets.UTF_8)+"'", 0);
            ITUtilLogs.assertListLogs("?level="+URLEncoder.encode("SHIFT UPDATE", StandardCharsets.UTF_8), 60);
            ITUtilLogs.assertListLogs("?level='"+URLEncoder.encode("SHIFT UPDATE", StandardCharsets.UTF_8)+"'", 0);
            ITUtilLogs.assertListLogs("?level=?pdate", 54);
            ITUtilLogs.assertListLogs("?level=upd??e", 54);
            ITUtilLogs.assertListLogs("?level=*ate", 54);

            // ----------------------------------------------------------------------------------------------------
            // keyword combinations
            // ----------------------------------------------------------------------------------------------------

            // default
            //     unsupported search parameters are ignored
            ITUtilLogs.assertListLogs("?zxcv", 60);
            ITUtilLogs.assertListLogs("?zxcv=asdf", 60);

            // combinations
            ITUtilLogs.assertListLogs("?logbooks=*&description=maintenance", 17);
            ITUtilLogs.assertListLogs("?tags=*&description=maintenance", 12);
            ITUtilLogs.assertListLogs("?properties=..12345678A&phrase="+URLEncoder.encode("Start-up after maintenance", StandardCharsets.UTF_8), 3);
            ITUtilLogs.assertListLogs("?properties=..12345678C&phrase="+URLEncoder.encode("Start-up after maintenance", StandardCharsets.UTF_8), 3);
            ITUtilLogs.assertListLogs("?properties=..123*&phrase="+URLEncoder.encode("Start-up after maintenance", StandardCharsets.UTF_8), 3);
            ITUtilLogs.assertListLogs("?properties=..123*&description=maintenance", 17);
        } catch (Exception e) {
            fail();
        }

        // --------------------------------------------------------------------------------
        // tear down test fixture
        // --------------------------------------------------------------------------------

        ITTestFixture.tearDown();
    }

}
