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

package org.phoebus.olog;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.phoebus.olog.entity.Attachment;
import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Log.LogBuilder;
import org.phoebus.olog.entity.LogEntryGroupHelper;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.SearchResult;
import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests {@link org.phoebus.olog.entity.Log} resource endpoints. The authentication scheme used is the
 * hard coded user/userPass credentials. The {@link LogRepository} is mocked.
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class})})
@WebMvcTest(LogResource.class)
@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
public class LogResourceTest extends ResourcesTestBase {

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private LogbookRepository logbookRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private LogEntryValidator logEntryValidator;

    private static Log log1;
    private static Log log2;

    private static Logbook logbook1;
    private static Logbook logbook2;

    private static Tag tag1;

    private static Tag tag2;

    private static final Instant now = Instant.now();

    @BeforeAll
    public static void init() {
        logbook1 = new Logbook("name1", "user");
        logbook2 = new Logbook("name2", "user");

        tag1 = new Tag("tag1");
        tag2 = new Tag("tag2");

        log1 = LogBuilder.createLog()
                .id(1L)
                .owner("owner")
                .title("title")
                .withLogbooks(Set.of(logbook1, logbook2))
                .description("description1")
                .withTags(Set.of(tag1, tag2))
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
    void testGetLogById() throws Exception {
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
    void testGetLogByIdRepositoryThrowsException() throws Exception {
        when(logRepository.findById("1")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, ""));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/1");
        mockMvc.perform(request).andExpect(status().isNotFound());
        verify(logRepository, times(1)).findById("1");
        reset(logRepository);
    }

    @Test
    void testFindLogs() throws Exception {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.put("a", List.of("b"));

        when(logRepository.search(map)).thenAnswer(invocationOnMock -> new SearchResult(2, Arrays.asList(log1, log2)));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LOG_RESOURCE_URI)
                .params(map)
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();

        Iterable<Log> logs = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals(Long.valueOf(1L), logs.iterator().next().getId());

        verify(logRepository, times(1)).search(map);
        reset(logRepository);
    }

    @Test
    void testSearchLogs() throws Exception {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.put("a", List.of("b"));

        when(logRepository.search(map)).thenAnswer(invocationOnMock -> new SearchResult(2, Arrays.asList(log1, log2)));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/search")
                .params(map)
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();

        SearchResult searchResult = objectMapper.readValue(result.getResponse().getContentAsString(), SearchResult.class);
        assertEquals(2, searchResult.getHitCount());
        assertEquals(2, searchResult.getLogs().size());
    }

    @Test
    void testSearchLogsUnsupportedTemporals() throws Exception {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.put("start", List.of("2 years"));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/search")
                .params(map)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isBadRequest());

        map = new LinkedMultiValueMap<>();
        map.put("start", List.of("2 months"));

        get("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/search")
                .params(map)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    void testCreateLogUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_RESOURCE_URI)
                .content(objectMapper.writeValueAsString(log1))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateLog() throws Exception {
        Log log = LogBuilder.createLog()
                .id(1L)
                .owner("user")
                .title("title")
                .withLogbooks(Set.of(logbook1, logbook2))
                .withTags(Set.of(tag1, tag2))
                .source("description1")
                .createDate(now)
                .modifyDate(now)
                .level("Urgent")
                .build();
        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));
        when(logRepository.save(argThat(new LogMatcher(log)))).thenReturn(log);
        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_RESOURCE_URI)
                .content(objectMapper.writeValueAsString(log1))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        Log savedLog = objectMapper.readValue(result.getResponse().getContentAsString(), Log.class);
        assertEquals(Long.valueOf(1L), savedLog.getId());
        reset(logRepository);
    }

    /**
     * Basically only test the endpoint...
     *
     * @throws Exception
     */
    @Test
    void testUpdateExisting() throws Exception {
        Property property1 = new Property();
        property1.setName("prop1");
        property1.addAttributes(new Attribute("name1", "value1"));

        Log log = LogBuilder.createLog()
                .id(1L)
                .owner("user")
                .title("title")
                .withLogbooks(Set.of(logbook1, logbook2))
                .withTags(Set.of(tag1, tag2))
                .description("description1")
                .createDate(now)
                .level("Urgent")
                .setProperties(Sets.newSet(property1))
                .build();

        when(logRepository.findById("1")).thenReturn(Optional.of(log));
        when(logRepository.update(log)).thenReturn(log);

        MockMultipartFile log1 = new MockMultipartFile("logEntry", "","application/json", objectMapper.writeValueAsString(log).getBytes());

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.multipart(HttpMethod.POST,
                                "/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/1")
                        .file(log1)
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data")
                        .contentType(JSON);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        Log savedLog = objectMapper.readValue(result.getResponse().getContentAsString(), Log.class);
        assertEquals(Long.valueOf(1L), savedLog.getId());
    }

    @Test
    void testUpdateBadRequest() throws Exception {
        Property property1 = new Property();
        property1.setName("prop1");
        property1.addAttributes(new Attribute("name1", "value1"));

        Log log = LogBuilder.createLog()
                .id(2L)
                .owner("user")
                .title("title")
                .withLogbooks(Set.of(logbook1, logbook2))
                .withTags(Set.of(tag1, tag2))
                .description("description1")
                .createDate(now)
                .level("Urgent")
                .setProperties(Sets.newSet(property1))
                .build();

        when(logRepository.findById("1")).thenReturn(Optional.of(log));
        when(logRepository.update(log)).thenReturn(log);

        MockHttpServletRequestBuilder request = post("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/1")
                .content(objectMapper.writeValueAsString(log))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    /**
     * Tests only endpoint URL.
     *
     * @throws Exception
     */
    @Test
    void testGetAttachment() throws Exception {
        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LOG_RESOURCE_URI
                + "/attachments/1/attachmentName");
        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    void testCreateAttachmentUnauthroized() throws Exception {
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
    void testCreateAttachment() throws Exception {
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

    @Test
    void testCreateLogMultipart() throws Exception {
        Attachment attachment = new Attachment();
        attachment.setId("attachmentId");
        attachment.setFilename("filename1.txt");
        Log log = LogBuilder.createLog()
                .id(1L)
                .owner("user")
                .title("title")
                .withLogbooks(Set.of(logbook1, logbook2))
                .withTags(Set.of(tag1, tag2))
                .source("description1")
                .description("description1")
                .createDate(now)
                .modifyDate(now)
                .level("Urgent")
                .build();
        Set<Attachment> attachments = new HashSet<>();
        attachments.add(attachment);
        log.setAttachments(attachments);
        MockMultipartFile file1 =
                new MockMultipartFile("files", "filename1.txt", "text/plain", "some xml".getBytes());
        MockMultipartFile log1 = new MockMultipartFile("logEntry", "","application/json", objectMapper.writeValueAsString(log).getBytes());

        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));
        when(logRepository.save(argThat(new LogMatcher(log)))).thenReturn(log);
        when(logRepository.findById("1")).thenReturn(Optional.of(log));
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.multipart(HttpMethod.PUT,
                                "/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/multipart")
                .file(file1)
                .file(log1)
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data")
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        Log savedLog = objectMapper.readValue(result.getResponse().getContentAsString(), Log.class);
        assertEquals(Long.valueOf(1L), savedLog.getId());
        reset(logRepository);
    }

    @Test
    void testCreateLogMultipartNoAttachments() throws Exception {
        Log log = LogBuilder.createLog()
                .id(1L)
                .owner("user")
                .title("title")
                .withLogbooks(Set.of(logbook1, logbook2))
                .withTags(Set.of(tag1, tag2))
                .source("description1")
                .description("description1")
                .createDate(now)
                .modifyDate(now)
                .level("Urgent")
                .build();
        MockMultipartFile log1 = new MockMultipartFile("logEntry", "","application/json", objectMapper.writeValueAsString(log).getBytes());

        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));
        when(logRepository.save(argThat(new LogMatcher(log)))).thenReturn(log);
        when(logRepository.findById("1")).thenReturn(Optional.of(log));
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.multipart(HttpMethod.PUT,
                                "/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/multipart")
                        .file(log1)
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data")
                        .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        Log savedLog = objectMapper.readValue(result.getResponse().getContentAsString(), Log.class);
        assertEquals(Long.valueOf(1L), savedLog.getId());
        reset(logRepository);
    }

    @Test
    void testCreateLogMultipartFileAndAttachmentMismatch() throws Exception {
        Attachment attachment = new Attachment();
        attachment.setId("attachmentId");
        attachment.setFilename("filename1.txt");
        Attachment attachment2 = new Attachment();
        attachment2.setId("attachmentId2");
        attachment2.setFilename("filename2.txt");
        Log log = LogBuilder.createLog()
                .id(1L)
                .owner("user")
                .title("title")
                .withLogbooks(Set.of(logbook1, logbook2))
                .withTags(Set.of(tag1, tag2))
                .source("description1")
                .description("description1")
                .createDate(now)
                .modifyDate(now)
                .level("Urgent")
                .build();
        Set<Attachment> attachments = new HashSet<>();
        attachments.add(attachment);
        attachments.add(attachment2);
        log.setAttachments(attachments);
        MockMultipartFile file1 =
                new MockMultipartFile("files", "filename1.txt", "text/plain", "some xml".getBytes());
        MockMultipartFile log1 = new MockMultipartFile("logEntry", "","application/json", objectMapper.writeValueAsString(log).getBytes());

        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));
        when(logRepository.save(argThat(new LogMatcher(log)))).thenReturn(log);
        when(logRepository.findById("1")).thenReturn(Optional.of(log));
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.multipart(HttpMethod.PUT,
                                "/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/multipart")
                        .file(file1)
                        .file(log1)
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data")
                        .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isBadRequest());

        reset(logRepository);
    }

    /**
     * A matcher used to work around issues with {@link Log#equals(Object)} when using the mocks.
     */
    private static class LogMatcher implements ArgumentMatcher<Log> {
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

    @Test
    void testReplyInvalidLogEntryId() throws Exception {
        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));
        when(logRepository.findById("7"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to retrieve log"));
        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "?inReplyTo=7")
                .content(objectMapper.writeValueAsString(log1))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isBadRequest());
        reset(logRepository);
    }

    @Test
    void testReplyValidLogEntryId() throws Exception {
        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));
        when(logRepository.findById("7"))
                .thenReturn(Optional.of(Log.LogBuilder.createLog().id(7L).build()));
        Log log = Log.LogBuilder.createLog().id(1L).build();
        when(logRepository.save(Mockito.any(Log.class))).thenAnswer(invocationOnMock -> log);
        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "?inReplyTo=7")
                .content(objectMapper.writeValueAsString(log1))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isOk());
        reset(logRepository);
    }

    @Test
    void testGroupNonExistingLogEntryId() throws Exception {
        when(logRepository.findById("1")).thenReturn(Optional.of(Log.LogBuilder.createLog().build()));
        when(logRepository.findById("2")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));

        List<Long> ids = Arrays.asList(1L, 2L);

        MockHttpServletRequestBuilder request = post("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/group")
                .content(objectMapper.writeValueAsString(ids))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isBadRequest());

        reset(logRepository);
    }

    @Test
    void testGroupMultipleGroupIdsFound() throws Exception {
        Property logEntryGroupProperty1 = LogEntryGroupHelper.createNewLogEntryProperty();
        Log log1 = Log.LogBuilder.createLog().id(1L).setProperties(Set.of(logEntryGroupProperty1)).build();
        Property logEntryGroupProperty2 = LogEntryGroupHelper.createNewLogEntryProperty();
        Log log2 = Log.LogBuilder.createLog().id(2L).setProperties(Set.of(logEntryGroupProperty2)).build();
        when(logRepository.findById("1")).thenReturn(Optional.of(log1));
        when(logRepository.findById("2")).thenReturn(Optional.of(log2));

        List<Long> ids = Arrays.asList(1L, 2L);

        MockHttpServletRequestBuilder request = post("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/group")
                .content(objectMapper.writeValueAsString(ids))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isBadRequest());

        reset(logRepository);
    }

    @Test
    void testGroupWithExisting1() throws Exception {
        Property logEntryGroupProperty1 = LogEntryGroupHelper.createNewLogEntryProperty();
        Log log1 = Log.LogBuilder.createLog().id(1L).setProperties(Set.of(logEntryGroupProperty1)).build();
        Log log2 = Log.LogBuilder.createLog().id(2L).setProperties(Set.of(logEntryGroupProperty1)).build();
        when(logRepository.findById("1")).thenReturn(Optional.of(log1));
        when(logRepository.findById("2")).thenReturn(Optional.of(log2));

        List<Long> ids = Arrays.asList(1L, 2L);

        MockHttpServletRequestBuilder request = post("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/group")
                .content(objectMapper.writeValueAsString(ids))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isOk());

        reset(logRepository);
    }

    @Test
    void testGroupWithExisting2() throws Exception {
        Property logEntryGroupProperty1 = LogEntryGroupHelper.createNewLogEntryProperty();
        Log log1 = Log.LogBuilder.createLog().id(1L).setProperties(Set.of(logEntryGroupProperty1)).build();
        Log log2 = Log.LogBuilder.createLog().id(2L).build();
        when(logRepository.findById("1")).thenReturn(Optional.of(log1));
        when(logRepository.findById("2")).thenReturn(Optional.of(log2));

        List<Long> ids = Arrays.asList(1L, 2L);

        MockHttpServletRequestBuilder request = post("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/group")
                .content(objectMapper.writeValueAsString(ids))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isOk());

        reset(logRepository);
    }

    @Test
    void testGroupNoExisting() throws Exception {
        Log log1 = Log.LogBuilder.createLog().id(1L).build();
        Log log2 = Log.LogBuilder.createLog().id(2L).build();
        when(logRepository.findById("1")).thenReturn(Optional.of(log1));
        when(logRepository.findById("2")).thenReturn(Optional.of(log2));

        List<Long> ids = Arrays.asList(1L, 2L);

        MockHttpServletRequestBuilder request = post("/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/group")
                .content(objectMapper.writeValueAsString(ids))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isOk());

        reset(logRepository);
    }
}
