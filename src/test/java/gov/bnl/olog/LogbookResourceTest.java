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
package gov.bnl.olog;

import com.fasterxml.jackson.core.type.TypeReference;
import gov.bnl.olog.entity.Logbook;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests {@link Logbook} resource endpoints. The authentication scheme used is the
 * hard coded user/userPass credentials. The {@link LogbookRepository} is mocked.
 */

@RunWith(SpringRunner.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class})})
@WebMvcTest(LogbookResourceTest.class)
@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
@ActiveProfiles({"test"})
public class LogbookResourceTest extends ResourcesTestBase {

    @Autowired
    private LogbookRepository logbookRepository;

    private Logbook logbook1;
    private Logbook logbook2;


    @Before
    public void init() {
        logbook1 = new Logbook("name1", "user");
        logbook2 = new Logbook("name2", "user");
    }

    @Test
    public void testFindAll() throws Exception {
        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LOGBOOK_RESOURCE_URI);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Iterable<Logbook> logbooks = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Iterable<Logbook>>() {
                });
        assertEquals("name1", logbooks.iterator().next().getName());
        verify(logbookRepository, times(1)).findAll();
        reset(logbookRepository);
    }

    @Test
    public void testFindAllNoLogbooks() throws Exception {
        when(logbookRepository.findAll()).thenReturn(new ArrayList<>());

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.LOGBOOK_RESOURCE_URI);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Iterable<Logbook> logbooks = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Iterable<Logbook>>() {
                });
        assertFalse(logbooks.iterator().hasNext());
        verify(logbookRepository, times(1)).findAll();
        reset(logbookRepository);
    }

    @Test
    public void testFindLogbookByName() throws Exception {
        when(logbookRepository.findById("name1")).thenReturn(Optional.of(logbook1));

        MockHttpServletRequestBuilder request = get("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI +
                "/name1");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Logbook logbook = objectMapper.readValue(result.getResponse().getContentAsString(),
                Logbook.class);
        assertEquals("name1", logbook.getName());
        verify(logbookRepository, times(1)).findById("name1");
        reset(logbookRepository);
    }

    @Test
    public void testCreateLogbookUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI +
                "/name1")
                .content(objectMapper.writeValueAsString(logbook1))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testCreateLogbook() throws Exception {
        Logbook logbookWithWrongOwnerName = new Logbook("name1", "owner1");
        when(logbookRepository.save(logbook1)).thenReturn(logbook1);
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI +
                "/name1")
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .content(objectMapper.writeValueAsString(logbookWithWrongOwnerName))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        // Verify that the log repository mock has been called with the owner field set to "user", and not "owner1"
        verify(logbookRepository, times(1)).save(logbook1);
        reset(logbookRepository);
    }

    @Test
    public void testUpdateLogbooksUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI)
                .content(objectMapper.writeValueAsString(new ArrayList<>()))
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateLogbooks() throws Exception {
        List<Logbook> logbooks = Arrays.asList(logbook1, logbook2);
        when(logbookRepository.saveAll(logbooks)).thenReturn(logbooks);
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI)
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .content(objectMapper.writeValueAsString(logbooks))
                .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        logbooks = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<Logbook>>() {
                });
        assertEquals("name1", logbooks.iterator().next().getName());
        verify(logbookRepository, times(1)).saveAll(logbooks);
        reset(logbookRepository);
    }

    @Test
    public void testDeleteUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI +
                "/name1");
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testDelete() throws Exception {
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.LOGBOOK_RESOURCE_URI +
                "/name1")
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isOk());
        verify(logbookRepository, times(1)).deleteById("name1");
        reset(logbookRepository);
    }
}
