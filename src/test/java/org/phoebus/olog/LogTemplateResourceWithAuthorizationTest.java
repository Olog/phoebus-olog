/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 */

package org.phoebus.olog;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.phoebus.olog.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests {@link LogTemplate} resource endpoints. The authentication scheme used is the
 * hard coded user/userPass credentials. The {@link LogTemplateRepository} is mocked.
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class})})
@WebMvcTest(LogResource.class)
@TestPropertySource(locations = "classpath:test_application_with_authorization.properties")
@SuppressWarnings("unused")
public class LogTemplateResourceWithAuthorizationTest extends ResourcesTestBase {

    private static final Logger log = LoggerFactory.getLogger(LogTemplateResourceWithAuthorizationTest.class);
    @Autowired
    private LogTemplateRepository logTemplateRepository;

    @Autowired
    private LogbookRepository logbookRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    private static Logbook logbook1;
    private static Logbook logbook2;

    private static Tag tag1;
    private static Tag tag2;

    private static LogTemplate logTemplate1;

    private static final String logTemplate1Id = UUID.randomUUID().toString();

    @BeforeAll
    public static void init() {
        logbook1 = new Logbook("name1", "user");
        logbook2 = new Logbook("name2", "user");

        tag1 = new Tag("tag1");
        tag2 = new Tag("tag2");

        logTemplate1 = new LogTemplate();
        logTemplate1.setName("name");
        logTemplate1.setId(logTemplate1Id);
        logTemplate1.setOwner("user");
        logTemplate1.setTitle("title");
        logTemplate1.setSource("source");
        logTemplate1.setLevel("Urgent");
        logTemplate1.setLogbooks(Set.of(logbook1, logbook2));
        logTemplate1.setTags(Set.of(tag1, tag2));
    }

    @Test
    void testCreateLogTemplateUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI)
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION)
                .content(objectMapper.writeValueAsString(new LogTemplate()))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    void testCreateLogTemplate() throws Exception {

        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));
        when(logTemplateRepository.save(argThat(new LogTemplateMatcher(logTemplate1)))).thenReturn(logTemplate1);
        MockHttpServletRequestBuilder request = put("/" + OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI)
                .content(objectMapper.writeValueAsString(logTemplate1))
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION)
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        LogTemplate savedLogTemplate = objectMapper.readValue(result.getResponse().getContentAsString(), LogTemplate.class);
        assertEquals(logTemplate1Id, savedLogTemplate.getId());
        reset(logbookRepository);
        reset(tagRepository);
        reset(logTemplateRepository);
    }

    @Test
    public void testDeleteUnauthorized() throws Exception{
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI + "/id")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    public void testDelete() throws Exception{
        when(logTemplateRepository.findById(logTemplate1Id)).thenReturn(Optional.of(logTemplate1));
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI + "/" + logTemplate1Id)
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isOk());
        verify(logTemplateRepository, times(1)).deleteById(logTemplate1Id);
        reset(logTemplateRepository);
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
