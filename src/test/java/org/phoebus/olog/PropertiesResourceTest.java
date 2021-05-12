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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.phoebus.olog.OlogResourceDescriptors;
import org.phoebus.olog.PropertyRepository;
import org.phoebus.olog.entity.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class})})
@WebMvcTest(LogbookResourceTest.class)
@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
public class PropertiesResourceTest extends ResourcesTestBase {

    @Autowired
    private PropertyRepository propertyRepository;

    private Property property1;
    private Property property2;

    @Before
    public void init() {
        property1 = new Property("property1");
        property2 = new Property("property2");
    }

    @Test
    public void testFindAll() throws Exception {
        when(propertyRepository.findAll()).thenReturn(Arrays.asList(property1, property2));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.PROPERTY_RESOURCE_URI);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Iterable<Property> properties = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<Iterable<Property>>() {
        });
        assertEquals("property1", properties.iterator().next().getName());
        verify(propertyRepository, times(1)).findAll();
        reset(propertyRepository);
    }

    @Test
    public void testFindById() throws Exception {
        when(propertyRepository.findById("property1")).thenReturn(Optional.of(property1));
        MockHttpServletRequestBuilder request = get("/" +
                OlogResourceDescriptors.PROPERTY_RESOURCE_URI +
                "/property1");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Property property = objectMapper.readValue(result.getResponse().getContentAsString(), Property.class);
        assertEquals("property1", property.getName());
        verify(propertyRepository, times(1)).findById("property1");
        reset(propertyRepository);
    }

    @Test
    public void testCreatePropertyUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.PROPERTY_RESOURCE_URI +
                "/property1");
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testCreateProperty() throws Exception {

        Property property = new Property("property1");
        property.setOwner("user");

        when(propertyRepository.save(argThat(new PropertyMatcher(property)))).thenReturn(property);

        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.PROPERTY_RESOURCE_URI +
                "/property1")
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(property1));
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        objectMapper.readValue(result.getResponse().getContentAsString(), Property.class);
        verify(propertyRepository, times(1)).save(property);
        reset(propertyRepository);
    }

    @Test
    public void testUpdatePropertyUnauthoroized() throws Exception {

        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.PROPERTY_RESOURCE_URI)
                .session(new MockHttpSession());
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateProperty() throws Exception {

        Property property = new Property("property1");
        property.setOwner("user");

        when(propertyRepository.saveAll(Arrays.asList(argThat(new PropertyMatcher(property)))))
                .thenReturn(Arrays.asList(property));

        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.PROPERTY_RESOURCE_URI)
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(Arrays.asList(property1)));
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<Iterable<Property>>() {
        });
        verify(propertyRepository, times(1)).saveAll(Arrays.asList(property));
        reset(propertyRepository);
    }

    @Test
    public void testDeleteUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.PROPERTY_RESOURCE_URI +
                "/property1");
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testDelete() throws Exception {
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.PROPERTY_RESOURCE_URI +
                "/property1")
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isOk());
        verify(propertyRepository, times(1)).deleteById("property1");
    }


    /**
     * A matcher used to work around issues with {@link Property#equals(Object)} when using the mocks.
     */
    private class PropertyMatcher implements ArgumentMatcher<Property> {
        private final Property expected;

        public PropertyMatcher(Property expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Property obj) {
            if (!(obj instanceof Property)) {
                return false;
            }
            Property actual = (Property) obj;

            return actual.getName().equals(expected.getName());
        }
    }
}
