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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.servlet.http.Cookie;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.phoebus.olog.OlogResourceDescriptors.OLOG_SERVICE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextHierarchy({@ContextConfiguration(classes = {AuthenticationResourceTestConfig.class})})
@WebMvcTest(AuthenticationResourceInifiniteSessionTest.class)
@TestPropertySource(locations = "classpath:no_ldap_test_application_infinite_session_duration.properties")
class AuthenticationResourceInifiniteSessionTest extends ResourcesTestBase {


    @Autowired
    private AuthenticationManager authenticationManager;

    @Test
    void testSuccessfullLogin() throws Exception {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ADMIN");
        Authentication mockAuthentication = mock(Authentication.class);
        Set authorities = new HashSet();
        authorities.add(authority);
        when(mockAuthentication.getAuthorities()).thenReturn(authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", "adminPass");
        when(authenticationManager.authenticate(authentication)).thenReturn(mockAuthentication);
        MockHttpServletRequestBuilder request = post("/" + OLOG_SERVICE + "/login?username=admin&password=adminPass");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("SESSION");
        assertNotNull(cookie);
        // Cookie max-age should be one year as it's set to negative number in properties file.
        assertEquals(AuthenticationResource.ONE_YEAR, cookie.getMaxAge());
    }
}
