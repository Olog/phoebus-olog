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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real security configuration with OIDC enabled against a (never contacted) identity provider.
 */
@SpringJUnitWebConfig(classes = {OidcLoginFlowTest.TestConfig.class, WebSecurityConfig.class,
        OidcSecurityConfig.class, AuthenticationResource.class})
@TestPropertySource(properties = {"authenticationProviders=oidc", "spring.session.timeout=30"})
class OidcLoginFlowTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestConfig {
        @Bean
        public ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("test")
                    .clientId("olog")
                    .clientSecret("secret")
                    .clientName("Test IdP")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/Olog/login/oauth2/code/{registrationId}")
                    .scope("openid")
                    .authorizationUri("https://idp.example.org/authorize")
                    .tokenUri("https://idp.example.org/token")
                    .jwkSetUri("https://idp.example.org/jwks")
                    .userNameAttributeName("preferred_username")
                    .build());
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void authorizationRequestRedirectsToIdentityProvider() throws Exception {
        String location = mockMvc.perform(get("/Olog/oauth2/authorization/test"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        String decoded = URLDecoder.decode(location, StandardCharsets.UTF_8);
        assertTrue(decoded.startsWith("https://idp.example.org/authorize?"), decoded);
        assertTrue(decoded.contains("client_id=olog"), decoded);
        assertTrue(decoded.contains("redirect_uri=http://localhost/Olog/login/oauth2/code/test"), decoded);
        assertTrue(decoded.contains("scope=openid"), decoded);
    }

    @Test
    void callbackWithoutAuthorizationRequestRedirectsToFailurePage() throws Exception {
        mockMvc.perform(get("/Olog/login/oauth2/code/test").param("code", "x").param("state", "unknown"))
                .andExpect(redirectedUrl("/?sso-error"));
    }

    @Test
    void publicEndpointsStayPublic() throws Exception {
        // No session cookie: the endpoint itself answers 404, security does not interfere
        mockMvc.perform(get("/Olog/user")).andExpect(status().isNotFound());
    }

    @Test
    void protectedEndpointsStillRequireAuthentication() throws Exception {
        mockMvc.perform(put("/Olog/logs")).andExpect(status().isUnauthorized());
    }
}
