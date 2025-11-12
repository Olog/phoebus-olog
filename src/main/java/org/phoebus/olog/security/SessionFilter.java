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

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.phoebus.olog.TextUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Filter that will take care of authenticating requests that come with either a session cookie or
 * a basic authentication header.
 * <p>
 * This class should not be instantiated as a bean in the application configuration. If it is, the
 * <code>doFilter()</code> method will be called for each endpoint URI, effectively defeating the purpose of the
 * configuration of ignored URI patterns set up in the Spring Security context, see
 * {@link WebSecurityConfig#configure(WebSecurity)}.
 */
public class SessionFilter extends GenericFilterBean {

    private AuthenticationManager authenticationManager;
    private FindByIndexNameSessionRepository sessionRepository;
    private ObjectMapper objectMapper;

    public SessionFilter(AuthenticationManager authenticationManager, FindByIndexNameSessionRepository sessionRepository) {
        this.authenticationManager = authenticationManager;
        this.sessionRepository = sessionRepository;
        objectMapper = new ObjectMapper();
    }

    /**
     * The request is authenticated as follows:
     * <ol>
     *     <li>If the request contains a cookie named SESSION, the session repository is queried to check if
     *     a session associated with the cookie value exists and is not expired. A non-expired session will be
     *     used to set the {@link Authentication} in the security context.</li>
     *     <li>If there is no SESSION cookie, or if the session associated with such a cookie is expired,
     *     the request is checked for a Authorization header. If it exists, its decoded username and password values
     *     are provided to the {@link AuthenticationManager} authentication. A successful authentication will then return
     *     the {@link Authentication} object that will passed to the security context.</li>
     *     <li>If none the previous steps is able to authenticate the request, <code>null</code> will be passed
     *     to the security context, i.e. request is not authenticated.</li>
     * </ol>
     *
     * @param request A {@link ServletRequest}
     * @param response A {@link ServletResponse}
     * @param filterChain The {@link FilterChain} to which this implementation contributes.
     * @throws IOException May be thrown by upstream filters.
     * @throws ServletException May be thrown by upstream filters.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        Authentication authentication = getAuthenticationFromCookie(httpServletRequest);
        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            String basicAuthenticationHeader = httpServletRequest.getHeader("Authorization");
            String[] usernameAndPassword = getUsernameAndPassword(basicAuthenticationHeader);
            if (usernameAndPassword == null) {
                SecurityContextHolder.getContext().setAuthentication(null);
            } else {
                authentication = new UsernamePasswordAuthenticationToken(usernameAndPassword[0],
                        usernameAndPassword[1]);
                try {
                    authentication = authenticationManager.authenticate(authentication);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (AuthenticationException e) {
                    Logger.getLogger(SessionFilter.class.getName())
                            .log(Level.FINE, MessageFormat.format(TextUtil.USER_NOT_AUTHENTICATED_THROUGH_AUTHORIZATION_HEADER, usernameAndPassword[0]));
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    protected String[] getUsernameAndPassword(String authorization) {
        if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
            // Authorization: Basic base64credentials
            String base64Credentials = authorization.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            // credentials = username:password
            return credentials.split(":", 2);
        }
        return null;
    }

    /**
     * Returns a {@link Authentication} object if the <code>httpServletRequest</code> contains
     * a cookie associated with a non-expired session.
     *
     * @param httpServletRequest A {@link HttpServletRequest}
     * @return Amn {@link Authentication} object if client has sent a valid session cookie.
     */
    protected Authentication getAuthenticationFromCookie(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("SESSION".equals(cookie.getName())) {
                Session session = sessionRepository.findById(cookie.getValue());
                if (session == null) { // No need to check expired, repository does it.
                    // Do not break. The request may contain multiple cookies named SESSION.
                    continue;
                }
                // Update last access time, repository does not do it automatically.
                session.setLastAccessedTime(Instant.now());
                sessionRepository.save(session);
                List<String> roles = session.getAttribute(WebSecurityConfig.ROLES_ATTRIBUTE_NAME);
                String userName = session.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME);
                List<GrantedAuthority> grantedAuthorities =
                        roles.stream().map(role -> new SimpleGrantedAuthority(role)).collect(Collectors.toList());
                return new UsernamePasswordAuthenticationToken(userName,
                        null, grantedAuthorities);
            }
        }
        return null;
    }
}
