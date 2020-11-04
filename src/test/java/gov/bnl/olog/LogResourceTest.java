/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package gov.bnl.olog;

import com.fasterxml.jackson.core.type.TypeReference;
import gov.bnl.olog.entity.Log;
import gov.bnl.olog.entity.Log.LogBuilder;
import gov.bnl.olog.entity.Logbook;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests {@link gov.bnl.olog.entity.Log} resource endpoints. The authentication scheme used is the
 * hard coded user/userPass credentials. The {@link LogRepository} is mocked.
 */
@RunWith(SpringRunner.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class})})
@WebMvcTest(LogResource.class)
@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
public class LogResourceTest extends ResourcesTestBase {

    @Autowired
    private LogRepository logRepository;

    private Log log1;
    private Log log2;

    private Logbook logbook1;
    private Logbook logbook2;

    private Instant now = Instant.now();

    @Before
    public void init() {
        logbook1 = new Logbook("name1", "user");
        logbook2 = new Logbook("name2", "user");

        log1 = LogBuilder.createLog()
                .id(1L)
                .owner("owner")
                .withLogbooks(Set.of(logbook1, logbook2))
                .description("description1")
                .createDate(now)
                .level("Urgent")
                .build();

        log2 = LogBuilder.createLog()
                .id(2L)
                .owner("user")
                .withLogbooks(Set.of(logbook1, logbook2))
                .description("description2")
                .createDate(now)
                .level("Urgent")
                .build();

    }

    @Test
    public void testGetLogById() throws Exception {
        when(logRepository.findById("1")).thenAnswer(invocationOnMock -> Optional.of(log1));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/1");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Log log = objectMapper.readValue(result.getResponse().getContentAsString(), Log.class);
        assertEquals("description1", log.getDescription());
        verify(logRepository, times(1)).findById("1");
        reset(logRepository);
    }

    @Test
    public void testFindLogs() throws Exception {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.put("a", Arrays.asList("b"));

        when(logRepository.search(map)).thenAnswer(invocationOnMock -> Arrays.asList(log1, log2));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LOG_RESOURCE_URI)
                .params(map)
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();

        Iterable<Log> logs = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Iterable<Log>>() {
                });
        assertEquals(Long.valueOf(1L), logs.iterator().next().getId());
        verify(logRepository, times(1)).search(map);
        reset(logRepository);
    }

    @Test
    public void testCreateLogUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_RESOURCE_URI)
                .content(objectMapper.writeValueAsString(log1))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testCreateLog() throws Exception {
        Log log = LogBuilder.createLog()
                .id(1L)
                .owner("user")
                .withLogbooks(Set.of(logbook1, logbook2))
                .description("description1")
                .createDate(now)
                .level("Urgent")
                .build();

        when(logRepository.save(argThat(new LogMatcher(log)))).thenReturn(log);
        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_RESOURCE_URI)
                .content(objectMapper.writeValueAsString(log1))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        Log savedLog = objectMapper.readValue(result.getResponse().getContentAsString(), Log.class);
        assertEquals(Long.valueOf(1L), savedLog.getId());
        verify(logRepository, times(1)).save(argThat(new LogMatcher(log)));
        reset(logRepository);
    }

    /**
     * Tests only endpoint URL.
     *
     * @throws Exception
     */
    @Test
    public void testGetAttachment() throws Exception {
        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LOG_RESOURCE_URI
                + "/attachments/1/attachmentName");
        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    public void testCreateAttachmentUnauthroized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_RESOURCE_URI
                + "/attachments/1");
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    /**
     * Test only endpoint URI
     *
     * @throws Exception
     */
    @Test
    public void testCreateAttachment() throws Exception {
        when(logRepository.findById("1")).thenReturn(Optional.of(log1));
        MockMultipartFile file =
                new MockMultipartFile("file", "filename.txt", "text/plain", "some xml".getBytes());
        MockMultipartFile filename =
                new MockMultipartFile("filename", "filename.txt", "text/plain", "some xml".getBytes());
        MockMultipartFile fileMetadataDescription =
                new MockMultipartFile("fileMetadataDescription", "filename.txt", "text/plain", "some xml".getBytes());
        mockMvc.perform(MockMvcRequestBuilders.multipart("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/attachments/1")
                .file(file)
                .file(filename)
                .file(fileMetadataDescription)
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andExpect(status().is(200));
        reset(logRepository);
    }

    /**
     * Test only endpoint URI
     *
     * @throws Exception
     */
    @Test
    public void testCreateMultipleAttachments() throws Exception {
        when(logRepository.findById("1")).thenReturn(Optional.of(log1));
        MockMultipartFile file1 =
                new MockMultipartFile("file", "filename1.txt", "text/plain", "some xml".getBytes());
        MockMultipartFile file2 =
                new MockMultipartFile("file", "filename2.txt", "text/plain", "some xml".getBytes());
        mockMvc.perform(MockMvcRequestBuilders.multipart("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/attachments-multi/1")
                .file(file1)
                .file(file2)
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andExpect(status().is(200));
        reset(logRepository);
    }


    /**
     * A matcher used to work around issues with {@link Log#equals(Object)} when using the mocks.
     */
    private class LogMatcher implements ArgumentMatcher<Log> {
        private final Log expected;

        public LogMatcher(Log expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Log obj) {
            if (!(obj instanceof Log)) {
                return false;
            }
            Log actual = (Log) obj;

            boolean match = actual.getId() == expected.getId()
                    && actual.getDescription().equals(expected.getDescription());

            return match;
        }
    }
}
