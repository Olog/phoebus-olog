/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 */
package org.phoebus.olog;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.olog.entity.Logbook;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests {@link Logbook} resource endpoints using embedded LDAP with authorization.
 */

@ExtendWith(SpringExtension.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class})})
@WebMvcTest(LogbookResourceWithAuthorizationTest.class)
@TestPropertySource(locations = "classpath:test_application_with_authorization.properties")
@ActiveProfiles({"test"})
public class LogbookResourceWithAuthorizationTest extends ResourcesTestBase {

    @Autowired
    private LogbookRepository logbookRepository;

    private static Logbook logbook1;
    private static Logbook logbook2;


    @BeforeAll
    public static void init() {
        logbook1 = new Logbook("name1", "user");
        logbook2 = new Logbook("name2", "user");
    }

    @Test
    void testCreateLogbook() throws Exception {
        Logbook logbookWithWrongOwnerName = new Logbook("name1", "owner1");
        when(logbookRepository.save(logbook1)).thenReturn(logbook1);
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI +
                "/name1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION)
                .content(objectMapper.writeValueAsString(logbookWithWrongOwnerName))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        reset(logbookRepository);
    }

    @Test
    void testCreateLogbookUnauthorized() throws Exception {
        Logbook logbookWithWrongOwnerName = new Logbook("name1", "owner1");
        when(logbookRepository.save(logbook1)).thenReturn(logbook1);
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI +
                "/name1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION)
                .content(objectMapper.writeValueAsString(logbookWithWrongOwnerName))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isForbidden());
        reset(logbookRepository);
    }

    @Test
    void testUpdateLogbooks() throws Exception {
        List<Logbook> logbooks = Arrays.asList(logbook1, logbook2);
        when(logbookRepository.saveAll(logbooks)).thenReturn(logbooks);
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI)
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION)
                .content(objectMapper.writeValueAsString(logbooks))
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        logbooks = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals("name1", logbooks.getFirst().getName());
        verify(logbookRepository, times(1)).saveAll(logbooks);
        reset(logbookRepository);
    }

    @Test
    void testUpdateLogbooksUnauthorized() throws Exception {
        List<Logbook> logbooks = Arrays.asList(logbook1, logbook2);
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI)
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION)
                .content(objectMapper.writeValueAsString(logbooks))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isForbidden());
        reset(logbookRepository);
    }

    @Test
    void testDelete() throws Exception {
        when(logbookRepository.findById("name1")).thenReturn(Optional.of(logbook1));
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI +
                "/name1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isOk());
        verify(logbookRepository, times(1)).deleteById("name1");
        reset(logbookRepository);
    }

    @Test
    void testDeleteUnauthorized() throws Exception {
        when(logbookRepository.findById("name1")).thenReturn(Optional.of(logbook1));
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI +
                "/name1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isForbidden());
        reset(logbookRepository);
    }
}
