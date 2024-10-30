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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.olog.entity.UserData;
import org.phoebus.olog.security.LoginCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.servlet.http.Cookie;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.phoebus.olog.OlogResourceDescriptors.OLOG_SERVICE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextHierarchy({@ContextConfiguration(classes = {AuthenticationResourceTestConfig.class})})
@WebMvcTest(AuthenticationResourceTest.class)
@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
class AuthenticationResourceTest extends ResourcesTestBase {


    @Autowired
    private AuthenticationManager authenticationManager;

    @Value("${spring.session.timeout}")
    private int sessionTimeout;

    @Test
    void testSuccessfullLogin() throws Exception {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ADMIN");
        Authentication mockAuthentication = mock(Authentication.class);
        Set authorities = new HashSet();
        authorities.add(authority);
        when(mockAuthentication.getAuthorities()).thenReturn(authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", "adminPass");
        when(authenticationManager.authenticate(authentication)).thenReturn(mockAuthentication);
        MockHttpServletRequestBuilder request = post("/" + OLOG_SERVICE + "/login")
                .contentType(JSON).content(objectMapper.writeValueAsString(new LoginCredentials("admin", "adminPass")));
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("SESSION");
        assertNotNull(cookie);
        // Cookie max-age should be same as spring.session.timeout, in seconds
        assertEquals(60 * sessionTimeout, cookie.getMaxAge());
        String content = result.getResponse().getContentAsString();
        UserData userData =
                new ObjectMapper().readValue(content, UserData.class);
        assertEquals("admin", userData.getUserName());
        assertNotNull(userData.getRoles());

        // Log in again and verify that the cookie value is the same, i.e. same session on server.
        when(mockAuthentication.getAuthorities()).thenReturn(authorities);
        when(authenticationManager.authenticate(authentication)).thenReturn(mockAuthentication);
        request = post("/" + OLOG_SERVICE + "/login")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(new LoginCredentials("admin", "adminPass")));
        result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Cookie cookie2 = result.getResponse().getCookie("SESSION");
        assertEquals(cookie.getValue(), cookie2.getValue());

        request = get("/" + OLOG_SERVICE + "/user").cookie(cookie);
        result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        content = result.getResponse().getContentAsString();
        userData =
                new ObjectMapper().readValue(content, UserData.class);
        assertEquals("admin", userData.getUserName());
        assertNotNull(userData.getRoles());

        reset(authenticationManager);
    }

    @Test
    void testGetUserWithNoCookie() throws Exception {
        MockHttpServletRequestBuilder request = get("/user");
        mockMvc.perform(request).andExpect(status().isNotFound());
    }

    @Test
    void testGetUserNoSession() throws Exception {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ADMIN");
        Authentication mockAuthentication = mock(Authentication.class);
        Set authorities = new HashSet();
        authorities.add(authority);
        when(mockAuthentication.getAuthorities()).thenReturn(authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", "adminPass");
        when(authenticationManager.authenticate(authentication)).thenReturn(mockAuthentication);
        MockHttpServletRequestBuilder request = post("/" + OLOG_SERVICE + "/login")
                .contentType("application/json").content(objectMapper.writeValueAsString(new LoginCredentials("admin", "adminPass")));
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();

        Cookie cookie = new Cookie("SESSION", "cookieValue");
        request = get("/user").cookie(cookie);
        mockMvc.perform(request).andExpect(status().isNotFound());

        reset(authenticationManager);
    }

    @Test
    void testFailedFormLogin() throws Exception {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ADMIN");
        Authentication mockAuthentication = mock(Authentication.class);
        Set authorities = new HashSet();
        authorities.add(authority);
        when(mockAuthentication.getAuthorities()).thenReturn(authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", "adminPass");
        when(authenticationManager.authenticate(authentication)).thenReturn(mockAuthentication);
        RequestBuilder requestBuilder = formLogin("/" + OLOG_SERVICE + "/login").acceptMediaType(MediaType.APPLICATION_JSON).user("admin").password("adminPass");
        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isBadRequest());
        reset(authenticationManager);
    }

    @Test
    void testFailedLogin() throws Exception {
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any(Authentication.class));
        MockHttpServletRequestBuilder request = post("/" + OLOG_SERVICE + "/login")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(new LoginCredentials("admin", "badPass")));
        mockMvc.perform(request).andExpect(status().isUnauthorized());
        reset(authenticationManager);
    }

    @Test
    void testLogout() throws Exception {
        MockHttpServletRequestBuilder request = get("/" + OLOG_SERVICE + "/logout");
        mockMvc.perform(request).andExpect(status().isOk());

        request = get("/" + OLOG_SERVICE + "/logout").cookie(new Cookie("SESSION", "abc"));
        mockMvc.perform(request).andExpect(status().isOk());
    }
}
