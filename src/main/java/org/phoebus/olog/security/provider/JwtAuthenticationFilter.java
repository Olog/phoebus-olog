package org.phoebus.olog.security.provider;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JwtAuthenticationFilter extends BasicAuthenticationFilter {

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwtToken = authorizationHeader.substring(7);

            try {
                // Pass the JWT token to the AuthenticationManager
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(null, jwtToken);
                SecurityContextHolder.getContext().setAuthentication(getAuthenticationManager().authenticate(authToken));
            } catch (Exception e) {
                logger.error("JWT authentication failed: " + e.getMessage());
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else if (authorizationHeader != null && authorizationHeader.startsWith("Basic ")) {
            // Service accounts (backup, exports) keep using Basic auth against the
            // non-JWT providers (ldap, embeddedLdap, inMemory) when OAuth2 is enabled.
            try {
                String decoded = new String(Base64.getDecoder().decode(authorizationHeader.substring(6)), StandardCharsets.UTF_8);
                int colon = decoded.indexOf(':');
                if (colon < 0) {
                    throw new IllegalArgumentException("Malformed Basic credentials");
                }
                Authentication authentication = getAuthenticationManager().authenticate(
                        new UsernamePasswordAuthenticationToken(decoded.substring(0, colon), decoded.substring(colon + 1)));
                if (authentication == null) {
                    throw new IllegalArgumentException("No provider accepted the credentials");
                }
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                logger.error("Basic authentication failed: " + e.getMessage());
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
