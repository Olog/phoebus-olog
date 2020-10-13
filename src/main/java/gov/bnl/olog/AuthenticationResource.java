/**
 * Copyright (C) 2020 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package gov.bnl.olog;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.bnl.olog.entity.UserData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/")
public class AuthenticationResource {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private FindByIndexNameSessionRepository sessionRepository;

    @Value("${spring.session.timeout:30}")
    private int sessionTimeout;

    private ObjectMapper objectMapper = new ObjectMapper();

    public static final int ONE_YEAR = 60 * 60 * 24 * 365;

    /**
     * Authenticates user and creates a session if authentication is successful.
     * A cookie named "SESSION" is created and provided in the response.
     * This endpoint can be used by a form-based login, or a POST where username
     * and password are specified as request parameters.
     *
     * @param userName The user principal name
     * @param password User's password
     * @param response {@link HttpServletResponse} to which a session cookie is
     *                 attached upon successful authentication.
     * @return A {@link ResponseEntity} carrying a {@link UserData} object if the login was successful,
     * otherwise the body will be <code>null</code>.
     */
    @PostMapping(value = "login")
    public ResponseEntity<UserData> login(@RequestParam(value = "username") String userName,
                                          @RequestParam(value = "password") String password,
                                          HttpServletResponse response) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(userName, password);
        try {
            authentication = authenticationManager.authenticate(authentication);
        } catch (AuthenticationException e) {
            return new ResponseEntity<>(
                    null,
                    HttpStatus.UNAUTHORIZED);
        }
        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).collect(Collectors.toList());
        Session session = findOrCreateSession(userName, roles);
        session.setLastAccessedTime(Instant.now());
        sessionRepository.save(session);
        Cookie cookie = new Cookie(WebSecurityConfig.SESSION_COOKIE_NAME, session.getId());
        if(sessionTimeout < 0){
            cookie.setMaxAge(ONE_YEAR); // Cannot set infinite on Cookie, so 1 year.
        }
        else{
            cookie.setMaxAge(60 * sessionTimeout); // sessionTimeout is in minutes.
        }
        response.addCookie(cookie);
        return new ResponseEntity<>(
                new UserData(userName, roles),
                HttpStatus.OK);
    }

    /**
     * Deletes a session identified by the session cookie, if present in the request.
     *
     * @param cookieValue
     */
    @GetMapping(value = "logout")
    public ResponseEntity<String> logout(@CookieValue(value = WebSecurityConfig.SESSION_COOKIE_NAME, required = false) String cookieValue) {
        if (cookieValue != null) {
            sessionRepository.deleteById(cookieValue);
        }
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    /**
     * Returns a {@link UserData} object populated with user name and roles. If the session cookie
     * is missing from the request, the {@link UserData} object fields are set to <code>null</code>.
     *
     * @param cookieValue
     * @return
     */
    @GetMapping(value = "user")
    public ResponseEntity<UserData> getCurrentUser(@CookieValue(value = WebSecurityConfig.SESSION_COOKIE_NAME,
            required = false) String cookieValue) {
        if (cookieValue == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Session session = sessionRepository.findById(cookieValue);
        if (session == null || session.isExpired()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        String userName = session.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME);
        List<String> roles = session.getAttribute(WebSecurityConfig.ROLES_ATTRIBUTE_NAME);
        return new ResponseEntity<>(new UserData(userName, roles), HttpStatus.OK);
    }

    /**
     * Creates a session or returns an existing one if a non-expired one is found in the session repository.
     * This is synchronized so that a user name is always associated with one session, irrespective of the
     * number of logins from clients.
     * @param userName
     * @param roles
     * @return
     */
    protected synchronized Session findOrCreateSession(String userName, List<String> roles){
        Session session;
        Map<String, Session> sessions = sessionRepository.findByPrincipalName(userName);
        if(!sessions.isEmpty()){
            // Get the first object in the map. Since a given user name should always use the same session,
            // the sessions maps should have only one element. However, an existing session may have
            // expired, so this must be checked as well.
            session = sessions.entrySet().iterator().next().getValue();
            if(session.isExpired()){
                sessionRepository.deleteById(session.getId());
            }
            else{
                return session;
            }
        }
        // No session found, create it.
        session = sessionRepository.createSession();
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, userName);
        session.setAttribute(WebSecurityConfig.ROLES_ATTRIBUTE_NAME, roles);
        session.setMaxInactiveInterval(Duration.ofMinutes(sessionTimeout));
        return session;
    }
}
