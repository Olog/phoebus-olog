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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.LogTemplate;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * Tests {@link LogTemplate} resource endpoints. The authentication scheme used is the
 * hard coded user/userPass credentials. The {@link LogTemplateRepository} is mocked.
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class})})
@WebMvcTest(LogResource.class)
@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
@SuppressWarnings("unused")
public class LogTemplateResourceTest extends ResourcesTestBase {

    private static final Logger log = LoggerFactory.getLogger(LogTemplateResourceTest.class);
    @Autowired
    private LogTemplateRepository logTemplateRepository;

    @Autowired
    private LogbookRepository logbookRepository;

    @Autowired
    private TagRepository tagRepository;

    private static Logbook logbook1;
    private static Logbook logbook2;

    private static Tag tag1;
    private static Tag tag2;

    @BeforeAll
    public static void init() {
        logbook1 = new Logbook("name1", "user");
        logbook2 = new Logbook("name2", "user");

        tag1 = new Tag("tag1");
        tag2 = new Tag("tag2");
    }

    @Test
    void testGetLogTemplateById() throws Exception {
        when(logTemplateRepository.findById("1")).thenAnswer(invocationOnMock -> Optional.of(new LogTemplate()));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI + "/1");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        LogTemplate log = objectMapper.readValue(result.getResponse().getContentAsString(), LogTemplate.class);
        assertNull(log.getName());
        verify(logTemplateRepository, times(1)).findById("1");
        reset(logTemplateRepository);
    }

    @Test
    void testGetLogTemplateIdNotFound() throws Exception {
        when(logTemplateRepository.findById("1")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, ""));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI + "/1");
        mockMvc.perform(request).andExpect(status().isNotFound());
        verify(logTemplateRepository, times(1)).findById("1");
        reset(logTemplateRepository);
    }

    @Test
    void testGetAllTemplates() throws Exception {

        when(logTemplateRepository.findAll()).thenAnswer(invocationOnMock -> List.of(new LogTemplate(), new LogTemplate()));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI)
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();

        Iterable<LogTemplate> logTemplates = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertTrue(logTemplates.iterator().hasNext());
        assertTrue(logTemplates.iterator().hasNext());

        verify(logTemplateRepository, times(1)).findAll();
        reset(logTemplateRepository);
    }

    @Test
    void testCreateLogTemplateUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI)
                .content(objectMapper.writeValueAsString(new LogTemplate()))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateLogTemplate() throws Exception {
        LogTemplate logTemplate = new LogTemplate();
        logTemplate.setName("name");
        String id = UUID.randomUUID().toString();
        logTemplate.setId(id);
        logTemplate.setOwner("user");
        logTemplate.setTitle("title");
        logTemplate.setSource("source");
        logTemplate.setLevel("Urgent");
        logTemplate.setLogbooks(Set.of(logbook1, logbook2));
        logTemplate.setTags(Set.of(tag1, tag2));

        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));
        when(logTemplateRepository.save(argThat(new LogTemplateMatcher(logTemplate)))).thenReturn(logTemplate);
        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI)
                .content(objectMapper.writeValueAsString(logTemplate))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        LogTemplate savedLogTemplate = objectMapper.readValue(result.getResponse().getContentAsString(), LogTemplate.class);
        assertEquals(id, savedLogTemplate.getId());
        reset(logbookRepository);
        reset(tagRepository);
        reset(logTemplateRepository);
    }

    @Test
    void testCreateLogTemplateBadLogbook() throws Exception {
        LogTemplate logTemplate = new LogTemplate();
        logTemplate.setName("name");
        logTemplate.setId(UUID.randomUUID().toString());
        logTemplate.setOwner("user");
        logTemplate.setTitle("title");
        logTemplate.setSource("description");
        logTemplate.setLevel("Urgent");
        logTemplate.setLogbooks(Set.of(logbook1));

        when(logbookRepository.findAll()).thenReturn(Collections.singletonList(logbook2));
        when(logTemplateRepository.save(argThat(new LogTemplateMatcher(logTemplate)))).thenReturn(logTemplate);
        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI)
                .content(objectMapper.writeValueAsString(logTemplate))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isBadRequest());
        reset(logbookRepository);
        reset(logTemplateRepository);
    }

    @Test
    void testCreateLogTemplateBadTags() throws Exception {
        LogTemplate logTemplate = new LogTemplate();
        logTemplate.setName("name");
        logTemplate.setId(UUID.randomUUID().toString());
        logTemplate.setOwner("user");
        logTemplate.setTitle("title");
        logTemplate.setSource("description");
        logTemplate.setLevel("Urgent");
        logTemplate.setTags(Set.of(tag1));

        when(tagRepository.findAll()).thenReturn(Collections.singletonList(tag2));
        when(logTemplateRepository.save(argThat(new LogTemplateMatcher(logTemplate)))).thenReturn(logTemplate);
        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI)
                .content(objectMapper.writeValueAsString(logTemplate))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isBadRequest());
        reset(tagRepository);
        reset(logTemplateRepository);
    }

    @Test
    void testCreateLogTemplateDuplicateName() throws Exception {
        LogTemplate existing = new LogTemplate();
        existing.setName("name");

        LogTemplate logTemplate = new LogTemplate();
        logTemplate.setName("Name");

        when(logTemplateRepository.findAll()).thenReturn(List.of(existing));

        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI)
                .content(objectMapper.writeValueAsString(logTemplate))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isBadRequest());
        reset(logTemplateRepository);
    }

    @Test
    @Disabled
    void testUpdateExisting() throws Exception {
        Property property1 = new Property();
        property1.setName("prop1");
        property1.addAttributes(new Attribute("name1", "value1"));

        LogTemplate logTemplate = new LogTemplate();
        logTemplate.setName("name");
        String id = UUID.randomUUID().toString();
        logTemplate.setId(id);
        logTemplate.setOwner("user");
        logTemplate.setTitle("title");
        logTemplate.setSource("description");
        logTemplate.setLevel("Urgent");
        logTemplate.setLogbooks(Set.of(logbook1, logbook2));
        logTemplate.setTags(Set.of(tag1, tag2));

        when(logTemplateRepository.findById(id)).thenReturn(Optional.of(logTemplate));
        when(logTemplateRepository.update(logTemplate)).thenReturn(logTemplate);

        MockHttpServletRequestBuilder request = post("/" + OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI + "/" + id)
                .content(objectMapper.writeValueAsString(logTemplate))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        LogTemplate savedLogTemplate = objectMapper.readValue(result.getResponse().getContentAsString(), LogTemplate.class);
        assertEquals(id, savedLogTemplate.getId());

        reset(logTemplateRepository);
    }

    @Test
    @Disabled
    void testUpdateExistingBadId() throws Exception {

        LogTemplate logTemplate = new LogTemplate();
        logTemplate.setName("name");
        logTemplate.setId(UUID.randomUUID().toString());

        when(logTemplateRepository.findById("1")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        MockHttpServletRequestBuilder request = post("/" + OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI + "/1")
                .content(objectMapper.writeValueAsString(logTemplate))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isNotFound());
        reset(logTemplateRepository);
    }

    @Test
    @Disabled
    void testUpdateNonExisting() throws Exception {

        LogTemplate logTemplate = new LogTemplate();
        logTemplate.setName("name");
        logTemplate.setId(UUID.randomUUID().toString());

        MockHttpServletRequestBuilder request = post("/" + OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI + "/2")
                .content(objectMapper.writeValueAsString(logTemplate))
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    /**
     * A matcher used to work around issues with {@link Log#equals(Object)} when using the mocks.
     */
    private static class LogTemplateMatcher implements ArgumentMatcher<LogTemplate> {
        private final LogTemplate expected;

        public LogTemplateMatcher(LogTemplate expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(LogTemplate obj) {
            if (obj == null) {
                return false;
            }

            return obj.getId().equals(expected.getId())
                    && obj.getSource().equals(expected.getSource());
        }
    }
}
