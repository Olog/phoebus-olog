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

package gov.bnl.olog;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.bnl.olog.entity.UserData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.servlet.http.Cookie;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextHierarchy({@ContextConfiguration(classes = {AuthenticationResourceTestConfig.class})})
@WebMvcTest(AuthenticationResourceTest.class)
@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
public class AuthenticationResourceTest extends ResourcesTestBase {


    @Autowired
    private AuthenticationManager authenticationManager;

    @Test
    public void testSuccessfullLogin() throws Exception {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ADMIN");
        Authentication mockAuthentication = mock(Authentication.class);
        Set authorities = new HashSet();
        authorities.add(authority);
        when(mockAuthentication.getAuthorities()).thenReturn(authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", "adminPass");
        when(authenticationManager.authenticate(authentication)).thenReturn(mockAuthentication);
        MockHttpServletRequestBuilder request = post("/login?username=admin&password=adminPass");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("SESSION");
        assertNotNull(cookie);
        String content = result.getResponse().getContentAsString();
        UserData userData =
                new ObjectMapper().readValue(content, UserData.class);
        assertEquals("admin", userData.getUserName());
        assertNotNull(userData.getRoles());

        // Log in again and verify that the cookie value is the same, i.e. same session on server.
        when(mockAuthentication.getAuthorities()).thenReturn(authorities);
        when(authenticationManager.authenticate(authentication)).thenReturn(mockAuthentication);
        request = post("/login?username=admin&password=adminPass");
        result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        Cookie cookie2 = result.getResponse().getCookie("SESSION");
        assertEquals(cookie.getValue(), cookie2.getValue());

        request = get("/user").cookie(cookie);
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
    public void testSuccessfullFormLogin() throws Exception {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ADMIN");
        Authentication mockAuthentication = mock(Authentication.class);
        Set authorities = new HashSet();
        authorities.add(authority);
        when(mockAuthentication.getAuthorities()).thenReturn(authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", "adminPass");
        when(authenticationManager.authenticate(authentication)).thenReturn(mockAuthentication);
        RequestBuilder requestBuilder = formLogin().acceptMediaType(MediaType.APPLICATION_JSON).user("admin").password("adminPass");
        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(cookie().exists("SESSION"));
        reset(authenticationManager);
    }

    @Test
    public void testFailedLogin() throws Exception {
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any(Authentication.class));
        MockHttpServletRequestBuilder request = post("/login?username=admin&password=badPass");
        mockMvc.perform(request).andExpect(status().isUnauthorized());
        reset(authenticationManager);
    }

    @Test
    public void testLogout() throws Exception {
        MockHttpServletRequestBuilder request = get("/logout");
        mockMvc.perform(request).andExpect(status().isOk());

        request = get("/logout").cookie(new Cookie("SESSION", "abc"));
        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    public void testGetCurrentUserReturningEmpty() throws Exception {
        MockHttpServletRequestBuilder request = get("/user");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        String content = result.getResponse().getContentAsString();
        UserData userData =
                new ObjectMapper().readValue(content, UserData.class);
        assertNull(userData.getUserName());
        assertNull(userData.getRoles());

        request = get("/logout").cookie(new Cookie("BAD", "abc"));
        result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        assertTrue(result.getResponse().getContentAsString().isEmpty());
    }
}
