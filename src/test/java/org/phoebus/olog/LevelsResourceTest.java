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
import org.phoebus.olog.entity.Level;
import org.phoebus.olog.entity.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class})})
@WebMvcTest(LevelsResourceTest.class)
@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
public class LevelsResourceTest extends ResourcesTestBase {

    @SuppressWarnings("unused")
    @Autowired
    private LevelRepository levelRepository;

    private static Level level1;
    private static Level level2;
    private static Level level3;

    @BeforeAll
    public static void init() {
        level1 = new Level("level1", true);
        level2 = new Level("level2", false);
        level3 = new Level("level3", true);
    }

    @Test
    void testFindAll() throws Exception {
        when(levelRepository.findAll()).thenReturn(Arrays.asList(level1, level2));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LEVEL_RESOURCE_RUI);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Iterable<Level> levels = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
        assertEquals("level1", levels.iterator().next().name());
        verify(levelRepository, times(1)).findAll();
        reset(levelRepository);
    }

    @Test
    void testFindById() throws Exception {
        when(levelRepository.findById("level1")).thenReturn(Optional.of(level1));
        MockHttpServletRequestBuilder request = get("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_RUI +
                "/level1");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Level level = objectMapper.readValue(result.getResponse().getContentAsString(), Level.class);
        assertEquals("level1", level.name());
        verify(levelRepository, times(1)).findById("level1");
        reset(levelRepository);
    }

    @Test
    void testFindByIdDoesNotExist() throws Exception {
        when(levelRepository.findById("level1")).thenReturn(Optional.empty());
        MockHttpServletRequestBuilder request = get("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_RUI +
                "/level1");
        mockMvc.perform(request).andExpect(status().isNotFound())
                .andReturn();
        verify(levelRepository, times(1)).findById("level1");
        reset(levelRepository);
    }

    @Test
    void testCreateLevelUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_RUI +
                "/level1");
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateLevel() throws Exception {

        when(levelRepository.save(argThat(new LevelMatcher(level1)))).thenReturn(level1);

        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_RUI +
                "/level1")
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(level1));
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        objectMapper.readValue(result.getResponse().getContentAsString(), Level.class);
        verify(levelRepository, times(1)).save(level1);
        reset(levelRepository);
    }

    @Test
    void testUpdateLevelUnauthoroized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_RUI)
                .session(new MockHttpSession());
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    void testSaveAll() throws Exception {

        when(levelRepository.saveAll(Collections.singletonList(argThat(new LevelMatcher(level1)))))
                .thenReturn(List.of(level1));

        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_RUI)
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonList(level1)));
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<Iterable<Level>>() {
        });
        verify(levelRepository, times(1)).saveAll(List.of(level1));
        reset(levelRepository);
    }

    @Test
    void testDeleteUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_RUI +
                "/level1");
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    void testDeleteNot() throws Exception {
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_RUI +
                "/level1")
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isNotFound());
        reset(levelRepository);
    }

    @Test
    void testDeleteNotFound() throws Exception {
        when(levelRepository.findById("level1")).thenReturn(Optional.of(level1));

        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_RUI +
                "/level1")
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isOk());
        verify(levelRepository, times(1)).findById("level1");
        verify(levelRepository, times(1)).deleteById("level1");

        reset(levelRepository);
    }

    @Test
    void testCrateFailOnDuplicateDefaults() throws Exception{
        when(levelRepository.findAll()).thenReturn(List.of(level1, level2));
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_RUI +
                "/level3")
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(level3));
        mockMvc.perform(request).andExpect(status().isBadRequest());
        verify(levelRepository, times(1)).findAll();
        reset(levelRepository);
    }

    @Test
    void testCrateFailOnNullName() throws Exception{
        Level level = new Level(null, false);
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_RUI +
                "/someLevel")
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(level));
        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    void testCrateFailOnEmptyName() throws Exception{
        Level level = new Level("", false);
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_RUI +
                "/someLevel")
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(level));
        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    /**
     * A matcher used to work around issues with {@link Property#equals(Object)} when using the mocks.
     */
    private static class LevelMatcher implements ArgumentMatcher<Level> {
        private final Level expected;

        public LevelMatcher(Level expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Level obj) {
            if (!(obj instanceof Level)) {
                return false;
            }
            Level actual = obj;

            return actual.name().equals(expected.name());
        }
    }
}
