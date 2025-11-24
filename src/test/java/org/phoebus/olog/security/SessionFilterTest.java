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

package org.phoebus.olog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SuppressWarnings("unused")
public class SessionFilterTest {

    private static AuthenticationManager authenticationManager =
            Mockito.mock(AuthenticationManager.class);

    private static FindByIndexNameSessionRepository sessionRepository =
            Mockito.mock(FindByIndexNameSessionRepository.class);

    private static SessionFilter sessionFilter;
    private static HttpServletResponse httpServletResponse;
    private static FilterChain filterChain;
    private static Cookie[] cookies;

    @BeforeAll
    public static void init() {
        sessionFilter = new SessionFilter(authenticationManager, sessionRepository);
        httpServletResponse = Mockito.mock(HttpServletResponse.class);
        filterChain = Mockito.mock(FilterChain.class);
        cookies = new Cookie[]{new Cookie("SESSION", "abc")};
    }

    @Test
    void testGetAuthorizationFromCookie() {
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        when(httpServletRequest.getCookies()).thenReturn(null);

        Authentication authentication = sessionFilter.getAuthenticationFromCookie(httpServletRequest);
        assertNull(authentication);

        reset(httpServletRequest);
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{});
        authentication = sessionFilter.getAuthenticationFromCookie(httpServletRequest);
        assertNull(authentication);

        reset(httpServletRequest);
        Cookie[] cookies = new Cookie[]{new Cookie(WebSecurityConfig.SESSION_COOKIE_NAME, "b"),
                new Cookie(WebSecurityConfig.SESSION_COOKIE_NAME, "a")};
        when(httpServletRequest.getCookies()).thenReturn(cookies);
        when(sessionRepository.findById("a")).thenReturn(null);
        authentication = sessionFilter.getAuthenticationFromCookie(httpServletRequest);
        assertNull(authentication);

        reset(httpServletRequest);
        when(httpServletRequest.getCookies()).thenReturn(cookies);
        Session session = new MapSession();
        session.setAttribute(WebSecurityConfig.ROLES_ATTRIBUTE_NAME, List.of("role1"));
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, "user");
        when(sessionRepository.findById("a")).thenReturn(session);
        authentication = sessionFilter.getAuthenticationFromCookie(httpServletRequest);
        assertNotNull(authentication);
    }

    @Test
    void testGetUsernameAndPasswordFromAuthorizationHeader() {
        assertNull(sessionFilter.getUsernameAndPassword(null));
        assertNull(sessionFilter.getUsernameAndPassword("Does not start with Basic"));

        String[] usernameAndPassword = sessionFilter.getUsernameAndPassword("Basic YWRtaW46YWRtaW5QYXNz");
        assertEquals("admin", usernameAndPassword[0]);
        assertEquals("adminPass", usernameAndPassword[1]);
    }

    @Test
    void testFilterWithoutCookieOrAuthenticationHeader() throws Exception {
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        when(httpServletRequest.getCookies()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        sessionFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{});
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        sessionFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testFilterWithNullSession() throws Exception {
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        when(httpServletRequest.getCookies()).thenReturn(cookies);
        when(sessionRepository.findById("abc")).thenReturn(null);
        sessionFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testFilterWithValidSession() throws Exception {
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        when(httpServletRequest.getCookies()).thenReturn(cookies);
        Session session = mock(Session.class);
        when(session.isExpired()).thenReturn(false);
        when(session.getAttribute(WebSecurityConfig.ROLES_ATTRIBUTE_NAME)).thenReturn(List.of("ROLE_ADMIN"));
        when(session.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME)).thenReturn("ADMIN");
        when(sessionRepository.findById("abc")).thenReturn(session);
        sessionFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("ADMIN", authentication.getName());
        assertEquals("ROLE_ADMIN", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void testFilterWithWrongAuthorizationHeader() throws Exception {
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        when(httpServletRequest.getCookies()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn("wrong header value");
        sessionFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testFilterWithInvalidAuthorizationHeader() throws Exception {
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        when(httpServletRequest.getCookies()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Basic YWRtaW46YWRtaW5QYXNz");
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any(Authentication.class));
        sessionFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testFilterWithValidAuthorizationHeader() throws Exception {
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        when(httpServletRequest.getCookies()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Basic YWRtaW46YWRtaW5QYXNz");
        Authentication authentication = new UsernamePasswordAuthenticationToken("ADMIN",
                "ADMIN_PASS");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        sessionFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("ADMIN", authentication.getName());
    }
}
