/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 */

package org.phoebus.olog;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
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
@WebMvcTest(LogbookResourceTest.class)
@TestPropertySource(locations = "classpath:test_application_with_authorization.properties")
public class PropertiesResourceWithAuthorizationTest extends ResourcesTestBase {

    @Autowired
    private PropertyRepository propertyRepository;

    private static Property property1;

    @BeforeAll
    public static void init() {
        property1 = new Property("property1");
    }

    @Test
    void testCreatePropertyUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.PROPERTY_RESOURCE_URI +
                "/property1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(property1));
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    void testCreateProperty() throws Exception {
        Property property = new Property("property1");
        property.setOwner("admin");

        when(propertyRepository.save(argThat(new PropertyMatcher(property)))).thenReturn(property);

        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.PROPERTY_RESOURCE_URI +
                "/property1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(property1));
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        objectMapper.readValue(result.getResponse().getContentAsString(), Property.class);
        verify(propertyRepository, times(1)).save(property);
        reset(propertyRepository);
    }

    @Test
    void testUpdatePropertyUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.PROPERTY_RESOURCE_URI + "/property1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(property1));
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    void testUpdateProperty() throws Exception {
        Property property = new Property("property1");
        property.setOwner("user");

        when(propertyRepository.saveAll(Collections.singletonList(argThat(new PropertyMatcher(property)))))
                .thenReturn(List.of(property));

        MockHttpServletRequestBuilder request = put("/" +
                OlogResourceDescriptors.PROPERTY_RESOURCE_URI)
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonList(property1)));
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<Iterable<Property>>() {
        });
        verify(propertyRepository, times(1)).saveAll(List.of(property));
        reset(propertyRepository);
    }

    @Test
    void testDeleteUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.PROPERTY_RESOURCE_URI +
                "/property1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_USER_AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    void testDelete() throws Exception {
        when(propertyRepository.findById("property1")).thenReturn(Optional.of(property1));
        MockHttpServletRequestBuilder request = delete("/" +
                OlogResourceDescriptors.PROPERTY_RESOURCE_URI +
                "/property1")
                .header(HttpHeaders.AUTHORIZATION, EMBEDDED_LDAP_ADMIN_AUTHORIZATION);
        mockMvc.perform(request).andExpect(status().isOk());
        verify(propertyRepository, times(1)).deleteById("property1");
        reset(propertyRepository);
    }

    /**
     * A matcher used to work around issues with {@link Property#equals(Object)} when using the mocks.
     */
    private static class PropertyMatcher implements ArgumentMatcher<Property> {
        private final Property expected;

        public PropertyMatcher(Property expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Property obj) {
            if (!(obj instanceof Property)) {
                return false;
            }
            return obj.getName().equals(expected.getName());
        }
    }
}
