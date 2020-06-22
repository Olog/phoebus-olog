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
     * @return A {@link ResponseEntity} indicating authentication result.
     */
    @PostMapping(value = "login")
    public ResponseEntity<String> login(@RequestParam(value = "username") String userName,
                                        @RequestParam(value = "password") String password,
                                        HttpServletResponse response) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(userName, password);
        try {
            authentication = authenticationManager.authenticate(authentication);
        } catch (AuthenticationException e) {
            return new ResponseEntity<>(
                    "Invalid credentials",
                    HttpStatus.UNAUTHORIZED);
        }
        Session session = sessionRepository.createSession();
        session.setMaxInactiveInterval(Duration.ofMinutes(sessionTimeout));
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, userName);
        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).collect(Collectors.toList());
        session.setAttribute(WebSecurityConfig.ROLES_ATTRIBUTE_NAME, roles);
        sessionRepository.save(session);
        Map<String, ? extends Session> sessionIds =
                sessionRepository.findByPrincipalName(userName);
        if (sessionIds.size() > 0) {
            session = sessionRepository.findById(sessionIds.keySet().iterator().next());
            Cookie cookie = new Cookie(WebSecurityConfig.SESSION_COOKIE_NAME, session.getId());
            response.addCookie(cookie);

        } else {
            return new ResponseEntity<>(
                    "Unable to create session for user",
                    HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(
                "",
                HttpStatus.OK);
    }

    /**
     * Deletes a session identified by the session cookie, if present in the request.
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
     * Will return the user name of the session as identified by the session cookie, or null if the
     * request does not contain the expected cookie or if the cookie is not associated with a non-expired session.
     * @param cookieValue
     * @return
     */
    @GetMapping(value = "user")
    public ResponseEntity<String> getCurrentUser(@CookieValue(value = WebSecurityConfig.SESSION_COOKIE_NAME, required = false) String cookieValue) {
        if (cookieValue == null) {
            return new ResponseEntity<>(null, HttpStatus.OK);
        }
        Session session = sessionRepository.findById(cookieValue);
        if (session == null || session.isExpired()) {
            return new ResponseEntity<>(null, HttpStatus.OK);
        }
        String userName = session.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME);
        return new ResponseEntity<>(userName, HttpStatus.OK);
    }
}
