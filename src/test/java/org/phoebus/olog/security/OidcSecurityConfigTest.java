/*
 * Copyright (C) 2026 The Olog contributors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.olog.AuthenticationResource;
import org.phoebus.olog.entity.AuthenticationOptions;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.Cookie;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OidcSecurityConfigTest {

    private OidcSecurityConfig config;
    private AuthenticationResource authenticationResource;
    private FindByIndexNameSessionRepository<Session> sessionRepository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        config = new OidcSecurityConfig();
        authenticationResource = mock(AuthenticationResource.class);
        sessionRepository = mock(FindByIndexNameSessionRepository.class);
        ReflectionTestUtils.setField(config, "authenticationResource", authenticationResource);
        ReflectionTestUtils.setField(config, "sessionRepository", sessionRepository);
        ReflectionTestUtils.setField(config, "postLoginRedirectUri", "/");
        ReflectionTestUtils.setField(config, "loginFailureRedirectUri", "/?sso-error");
        ReflectionTestUtils.setField(config, "sessionTimeout", 30);
        ReflectionTestUtils.setField(config, "browserPasswordLogin", true);
    }

    private static Set<String> names(Set<GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    @Test
    void toAuthoritiesFromList() {
        assertEquals(Set.of("ROLE_OLOG-ADMINS", "ROLE_OLOG-LOGS"),
                names(OidcSecurityConfig.toAuthorities(List.of("olog-admins", "olog-logs"), null)));
    }

    @Test
    void toAuthoritiesFromSingleValue() {
        assertEquals(Set.of("ROLE_OLOG-ADMINS"), names(OidcSecurityConfig.toAuthorities("olog-admins", null)));
    }

    @Test
    void toAuthoritiesStripsDomainPrefix() {
        assertEquals(Set.of("ROLE_OLOG-ADMINS"), names(OidcSecurityConfig.toAuthorities(List.of("EXAMPLE\\olog-admins"), null)));
    }

    @Test
    void toAuthoritiesAppliesFilter() {
        Pattern filter = Pattern.compile("(?i)olog-.*");
        assertEquals(Set.of("ROLE_OLOG-LOGS"),
                names(OidcSecurityConfig.toAuthorities(List.of("Domain Users", "Olog-Logs"), filter)));
    }

    @Test
    void toAuthoritiesWithoutClaim() {
        assertTrue(OidcSecurityConfig.toAuthorities(null, null).isEmpty());
        assertTrue(OidcSecurityConfig.toAuthorities(List.of(" ", "\\"), null).isEmpty());
    }

    @Test
    void conditionMatchesOnlyIfListed() {
        assertTrue(conditionMatches("ldap,oidc"));
        assertTrue(conditionMatches("ldap, oidc"));
        assertTrue(conditionMatches("oidc"));
        assertFalse(conditionMatches("ldap"));
        assertFalse(conditionMatches("oidcx"));
        assertFalse(conditionMatches(""));
        assertFalse(conditionMatches(null));
    }

    private static boolean conditionMatches(String providers) {
        MockEnvironment environment = new MockEnvironment();
        if (providers != null) {
            environment.setProperty(WebSecurityConfig.PROVIDER_LIST_PROPERTY_NAME, providers);
        }
        ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        return new OidcSecurityConfig.OidcCondition().matches(context, null);
    }

    private static ClientRegistration registration() {
        return ClientRegistration.withRegistrationId("test")
                .clientId("olog")
                .clientName("Test IdP")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/Olog/login/oauth2/code/{registrationId}")
                .authorizationUri("https://idp.example.org/authorize")
                .tokenUri("https://idp.example.org/token")
                .build();
    }

    @Test
    void authenticationOptionsListsProviders() {
        ReflectionTestUtils.setField(config, "clientRegistrationRepository", new InMemoryClientRegistrationRepository(registration()));
        AuthenticationOptions options = config.authenticationOptions();
        assertTrue(options.passwordLogin());
        assertEquals(List.of(new AuthenticationOptions.OidcProvider("test", "Test IdP")), options.oidcProviders());
    }

    @Test
    void authenticationOptionsCanHidePasswordLogin() {
        ReflectionTestUtils.setField(config, "clientRegistrationRepository", new InMemoryClientRegistrationRepository(registration()));
        ReflectionTestUtils.setField(config, "browserPasswordLogin", false);
        assertFalse(config.authenticationOptions().passwordLogin());
    }

    @Test
    void authenticationOptionsNeverHidePasswordLoginWithoutProviders() {
        // A repository that cannot be enumerated yields no providers
        ReflectionTestUtils.setField(config, "clientRegistrationRepository", mock(ClientRegistrationRepository.class));
        ReflectionTestUtils.setField(config, "browserPasswordLogin", false);
        AuthenticationOptions options = config.authenticationOptions();
        assertTrue(options.passwordLogin());
        assertTrue(options.oidcProviders().isEmpty());
    }

    @Test
    void successCreatesSessionCookieAndRedirects() throws Exception {
        MapSession session = new MapSession();
        when(authenticationResource.findOrCreateSession(eq("jdoe"), anyList())).thenReturn(session);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        request.getSession(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("jdoe", null,
                List.of(new SimpleGrantedAuthority("ROLE_OLOG-ADMINS")));

        config.onAuthenticationSuccess(request, response, authentication);

        assertEquals(List.of("ROLE_OLOG-ADMINS"), session.getAttribute(WebSecurityConfig.ROLES_ATTRIBUTE_NAME));
        verify(sessionRepository).save(session);
        Cookie cookie = response.getCookie(WebSecurityConfig.SESSION_COOKIE_NAME);
        assertNotNull(cookie);
        assertEquals(session.getId(), cookie.getValue());
        assertEquals("/Olog", cookie.getPath());
        assertEquals(30 * 60, cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals("/", response.getRedirectedUrl());
        assertTrue(request.getSession(false) == null, "servlet session must be invalidated");
    }

    @Test
    void failureRedirects() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        config.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationException("invalid_request"));
        assertEquals("/?sso-error", response.getRedirectedUrl());
    }
}
