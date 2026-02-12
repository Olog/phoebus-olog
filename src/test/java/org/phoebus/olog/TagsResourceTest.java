/*
 * Copyright (C) 2020 European Spallation Source ERIC.
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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests {@link Logbook} resource endpoints. The authentication scheme used is the
 * hard coded user/userPass credentials. The {@link LogbookRepository} is mocked.
 */

@ExtendWith(SpringExtension.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class})})
@WebMvcTest(TagsResourceTest.class)
@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
@ActiveProfiles({"test"})
public class TagsResourceTest extends ResourcesTestBase {

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
    void testFindAll() throws Exception {
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.TAG_RESOURCE_URI);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Iterable<Tag> tags = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals("tag1", tags.iterator().next().getName());
        verify(tagRepository, times(1)).findAll();
    }

    @Test
    void testFindAllNoTags() throws Exception {
        when(tagRepository.findAll()).thenReturn(new ArrayList<>());

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.TAG_RESOURCE_URI);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Iterable<Logbook> logbooks = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertFalse(logbooks.iterator().hasNext());
        verify(tagRepository, times(1)).findAll();
        reset(tagRepository);
    }

    @Test
    void testFindTagByName() throws Exception {
        when(tagRepository.findById("tag1")).thenReturn(Optional.of(tag1));

        MockHttpServletRequestBuilder request = get("/" +
                OlogResourceDescriptors.TAG_RESOURCE_URI +
                "/tag1");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Tag tag = objectMapper.readValue(result.getResponse().getContentAsString(),
                Tag.class);
        assertEquals("tag1", tag.getName());
        verify(tagRepository, times(1)).findById("tag1");
        reset(tagRepository);
    }

    @Test
    void testCreateTagUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.TAG_RESOURCE_URI +
                "/tag1")
                .content(objectMapper.writeValueAsString(tag1))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateTag() throws Exception {
        Tag tag = new Tag("tag");
        when(tagRepository.save(tag)).thenReturn(tag);
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.TAG_RESOURCE_URI +
                "/tag")
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .content(objectMapper.writeValueAsString(tag))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        reset(tagRepository);
    }

    @Test
    void testUpdateLogbooksUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI)
                .content(objectMapper.writeValueAsString(new ArrayList<>()))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    void testUpdateTags() throws Exception {
        List<Tag> tags = Arrays.asList(tag1, tag2);
        when(tagRepository.saveAll(tags)).thenReturn(tags);
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.TAG_RESOURCE_URI)
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .content(objectMapper.writeValueAsString(tags))
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        tags = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals("tag1", tags.iterator().next().getName());
        verify(tagRepository, times(1)).saveAll(tags);
        reset(tagRepository);
    }

    @Test
    void testDeleteUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.TAG_RESOURCE_URI +
                "/tag1");
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    void testDelete() throws Exception {
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.TAG_RESOURCE_URI +
                "/tag1")
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isNotFound());
        reset(tagRepository);
    }
}
