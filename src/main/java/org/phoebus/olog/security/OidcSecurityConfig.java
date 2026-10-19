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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.phoebus.olog.AuthenticationResource;
import org.phoebus.olog.OlogResourceDescriptors;
import org.phoebus.olog.entity.AuthenticationOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * OpenID Connect login for browser clients.
 *
 * <p>
 * Enabled by adding <code>oidc</code> to the comma separated property <code>authenticationProviders</code>,
 * e.g. <code>authenticationProviders=ldap,oidc</code>. Identity providers are configured with the standard
 * Spring Boot properties <code>spring.security.oauth2.client.registration.&lt;id&gt;.*</code> and
 * <code>spring.security.oauth2.client.provider.&lt;id&gt;.*</code>.
 * </p>
 * <p>
 * The browser is sent to <code>/Olog/oauth2/authorization/&lt;id&gt;</code>, the identity provider redirects back to
 * <code>/Olog/login/oauth2/code/&lt;id&gt;</code>. On success a session is created exactly as for the
 * username/password login (<code>POST /Olog/login</code>): the <code>SESSION</code> cookie and the session repository
 * are shared, so <code>/Olog/user</code>, <code>/Olog/logout</code> and the role checks work unchanged.
 * Username/password authentication (e.g. Basic Auth used by the Phoebus client) is not affected.
 * </p>
 * <p>
 * Roles are taken from a groups claim and mapped like the LDAP provider does it:
 * group <code>olog-admins</code> becomes authority <code>ROLE_OLOG-ADMINS</code>.
 * </p>
 */
@Configuration
@Conditional(OidcSecurityConfig.OidcCondition.class)
public class OidcSecurityConfig {

    public static final String OIDC = "oidc";
    public static final String AUTHORIZATION_BASE_URI = "/" + OlogResourceDescriptors.OLOG_SERVICE + "/oauth2/authorization";
    public static final String REDIRECTION_BASE_URI = "/" + OlogResourceDescriptors.OLOG_SERVICE + "/login/oauth2/code/*";

    private static final Logger logger = Logger.getLogger(OidcSecurityConfig.class.getName());

    /**
     * Claim holding the Olog user name. Should match what other providers use, otherwise the same person
     * shows up as different log entry owners depending on how they logged in.
     */
    @Value("${oidc.username-claim:preferred_username}")
    private String usernameClaim;

    /**
     * Claim holding the group names (string or array).
     */
    @Value("${oidc.groups-claim:groups}")
    private String groupsClaim;

    /**
     * Optional regular expression, only matching group names are mapped to roles. Empty means all groups.
     */
    @Value("${oidc.groups-filter:}")
    private String groupsFilter;

    /**
     * Hint for browser clients, see {@link AuthenticationOptions#passwordLogin()}.
     */
    @Value("${oidc.browser-password-login:true}")
    private boolean browserPasswordLogin;

    @Value("${oidc.post-login-redirect-uri:/}")
    private String postLoginRedirectUri;

    @Value("${oidc.login-failure-redirect-uri:/?sso-error}")
    private String loginFailureRedirectUri;

    @Value("${spring.session.timeout:30}")
    private int sessionTimeout;

    @Autowired
    private FindByIndexNameSessionRepository<Session> sessionRepository;

    @Autowired
    private AuthenticationResource authenticationResource;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    @Order(1)
    public SecurityFilterChain oidcFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(AUTHORIZATION_BASE_URI + "/**",
                        "/" + OlogResourceDescriptors.OLOG_SERVICE + "/login/oauth2/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                // Olog manages its own sessions, the security context must not end up in the servlet session.
                .securityContext(c -> c.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(c -> c.disable())
                .oauth2Login(o -> o
                        .authorizationEndpoint(a -> a.baseUri(AUTHORIZATION_BASE_URI))
                        .redirectionEndpoint(r -> r.baseUri(REDIRECTION_BASE_URI))
                        .userInfoEndpoint(u -> u.oidcUserService(oidcUserService()))
                        .successHandler(this::onAuthenticationSuccess)
                        .failureHandler(this::onAuthenticationFailure));
        return http.build();
    }

    /**
     * @return The configured identity providers and the password login hint, as advertised by the service
     * configuration endpoint. The password form is never hidden if no provider exists, so that users
     * cannot be locked out.
     */
    public AuthenticationOptions authenticationOptions() {
        List<AuthenticationOptions.OidcProvider> providers = new ArrayList<>();
        if (clientRegistrationRepository instanceof Iterable<?> registrations) {
            for (Object registration : registrations) {
                if (registration instanceof ClientRegistration r
                        && AuthorizationGrantType.AUTHORIZATION_CODE.equals(r.getAuthorizationGrantType())) {
                    providers.add(new AuthenticationOptions.OidcProvider(r.getRegistrationId(), r.getClientName()));
                }
            }
        }
        return new AuthenticationOptions(providers.isEmpty() || browserPasswordLogin, providers);
    }

    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();
        Pattern filter = groupsFilter == null || groupsFilter.isBlank() ? null : Pattern.compile(groupsFilter);
        return request -> {
            OidcUser user = delegate.loadUser(request);
            Set<GrantedAuthority> authorities = toAuthorities(user.getClaims().get(groupsClaim), filter);
            // Throws if the username claim is missing, which is the desired behavior
            return new DefaultOidcUser(authorities, user.getIdToken(), user.getUserInfo(), usernameClaim);
        };
    }

    /**
     * Maps the value of a groups claim to authorities, consistent with the LDAP provider.
     *
     * @param groupsClaim Claim value, a single string or a collection. Domain prefixes like
     *                    <code>DOMAIN\group</code> are removed.
     * @param filter      Optional filter, only matching group names are mapped. May be <code>null</code>.
     * @return Authorities in the form <code>ROLE_&lt;GROUP IN UPPER CASE&gt;</code>
     */
    static Set<GrantedAuthority> toAuthorities(Object groupsClaim, Pattern filter) {
        List<String> groups = new ArrayList<>();
        if (groupsClaim instanceof Collection<?> collection) {
            collection.forEach(g -> groups.add(String.valueOf(g)));
        } else if (groupsClaim != null) {
            groups.add(String.valueOf(groupsClaim));
        }
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (String group : groups) {
            String name = group.substring(group.lastIndexOf('\\') + 1).trim();
            if (!name.isEmpty() && (filter == null || filter.matcher(name).matches())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + name.toUpperCase()));
            }
        }
        return authorities;
    }

    void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                 Authentication authentication) throws IOException {
        String userName = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        Session session = authenticationResource.findOrCreateSession(userName, roles);
        // An existing session may carry outdated roles, use the ones from the fresh token.
        session.setAttribute(WebSecurityConfig.ROLES_ATTRIBUTE_NAME, roles);
        session.setLastAccessedTime(Instant.now());
        sessionRepository.save(session);

        Cookie cookie = new Cookie(WebSecurityConfig.SESSION_COOKIE_NAME, session.getId());
        cookie.setPath("/" + OlogResourceDescriptors.OLOG_SERVICE);
        cookie.setMaxAge(sessionTimeout < 0 ? AuthenticationResource.ONE_YEAR : 60 * sessionTimeout);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);

        // The servlet session was only needed for state and nonce of the authorization request.
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            httpSession.invalidate();
        }
        logger.log(Level.INFO, "OIDC login of user ''{0}'' with roles {1}", new Object[]{userName, roles});
        response.sendRedirect(postLoginRedirectUri);
    }

    void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                 AuthenticationException exception) throws IOException {
        logger.log(Level.WARNING, "OIDC login failed: " + exception.getMessage(), exception);
        response.sendRedirect(loginFailureRedirectUri);
    }

    /**
     * Matches if <code>oidc</code> is listed in property <code>authenticationProviders</code>.
     */
    public static class OidcCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String value = context.getEnvironment().getProperty(WebSecurityConfig.PROVIDER_LIST_PROPERTY_NAME);
            if (value == null || value.isEmpty()) {
                return false;
            }
            return Arrays.stream(value.split(",")).map(String::trim).anyMatch(OIDC::equals);
        }
    }
}
