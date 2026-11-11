/*
 * Copyright (C) 2026European Spallation Source ERIC.
 */
package org.phoebus.olog;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.State;
import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests {@link Logbook} resource endpoints using embedded LDAP with authorization.
 */

@ExtendWith(SpringExtension.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class})})
@WebMvcTest(TagsResourceWithAuthorizationTest.class)
@TestPropertySource(locations = "classpath:test_application_with_authorization.properties")
@ActiveProfiles({"test"})
public class TagsResourceWithAuthorizationTest extends ResourcesTestBase {

    @Autowired
    private TagRepository tagRepository;

    private static Tag tag1;
    private static Tag tag2;


    @BeforeAll
    public static void init() {
        tag1 = new Tag("tag1", State.Active);
        tag2 = new Tag("tag2");
    }

    @Test
    void testCreateTagUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.TAG_RESOURCE_URI +
                "/tag1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION)
                .content(objectMapper.writeValueAsString(tag1))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    void testCreateTag() throws Exception {
        Tag tag = new Tag("tag");
        when(tagRepository.save(tag)).thenReturn(tag);
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.TAG_RESOURCE_URI +
                "/tag")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION)
                .content(objectMapper.writeValueAsString(tag))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        reset(tagRepository);
    }

    @Test
    void testUpdateTagsUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.TAG_RESOURCE_URI)
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION)
                .content(objectMapper.writeValueAsString(new ArrayList<>()))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    void testUpdateTags() throws Exception {
        List<Tag> tags = Arrays.asList(tag1, tag2);
        when(tagRepository.saveAll(tags)).thenReturn(tags);
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.TAG_RESOURCE_URI)
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION)
                .content(objectMapper.writeValueAsString(tags))
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        tags = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals("tag1", tags.getFirst().getName());
        verify(tagRepository, times(1)).saveAll(tags);
        reset(tagRepository);
    }

    @Test
    void testDeleteUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.TAG_RESOURCE_URI +
                "/tag1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    void testDelete() throws Exception {
        when(tagRepository.findById("tag1")).thenReturn(Optional.of(tag1));
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.TAG_RESOURCE_URI +
                "/tag1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isOk());
        verify(tagRepository, times(1)).deleteById("tag1");
        reset(tagRepository);
    }
}
