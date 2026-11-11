/*
 * Copyright (C) 2026 European Spallation Source ERIC.
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class})})
@WebMvcTest(LevelsResourceWithAuthorizationTest.class)
@TestPropertySource(locations = "classpath:test_application_with_authorization.properties")
public class LevelsResourceWithAuthorizationTest extends ResourcesTestBase {

    @SuppressWarnings("unused")
    @Autowired
    private LevelRepository levelRepository;

    private static Level level1;

    @BeforeAll
    public static void init() {
        level1 = new Level("level1", true);
    }

    @Test
    void testCreateLevelUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_URI +
                "/level1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(level1));
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    void testCreateLevel() throws Exception {

        when(levelRepository.save(argThat(new LevelMatcher(level1)))).thenReturn(level1);

        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_URI +
                "/level1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(level1));
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        objectMapper.readValue(result.getResponse().getContentAsString(), Level.class);
        verify(levelRepository, times(1)).save(level1);
        reset(levelRepository);
    }

    @Test
    void testUpdateLevelUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_URI + "/level1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(level1));
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    void testSaveAll() throws Exception {
        when(levelRepository.saveAll(Collections.singletonList(argThat(new LevelMatcher(level1)))))
                .thenReturn(List.of(level1));

        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_URI)
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION)
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
                OlogResourceDescriptors.LEVEL_RESOURCE_URI +
                "/level1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    void testDelete() throws Exception {
        when(levelRepository.findById("level1")).thenReturn(Optional.of(level1));
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.LEVEL_RESOURCE_URI +
                "/level1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isOk());
        verify(levelRepository, times(1)).deleteById("level1");
        reset(levelRepository);
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

            return obj.name().equals(expected.name());
        }
    }
}
