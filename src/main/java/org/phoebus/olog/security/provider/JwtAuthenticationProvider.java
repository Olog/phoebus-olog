package org.phoebus.olog.security.provider;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;


import java.util.Collections;
import java.util.List;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.client.RestTemplate;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

/**
 * Custom authentication provider that verifies JWT tokens issued by an OIDC server.
 */
@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

    @Value("${oauth2.issueUri}")
    String issuerUri;

    @Value("${oauth2.claimsName}")
    String claimsName;
    private final RestTemplate restTemplate = new RestTemplate();


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String jwtToken = (String) authentication.getCredentials();

        try {
            // Check the token
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(fetchPublicKey())
                    .build()
                    .parseClaimsJws(jwtToken)
                    .getBody();

            String username = claims.get(claimsName, String.class);
            if (username == null) {
                throw new UsernameNotFoundException("Username not found in token");
            }

            // Assign the user the ROLE_USER role
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            return new UsernamePasswordAuthenticationToken(username, jwtToken, authorities);

        } catch (Exception e) {
            throw new AuthenticationServiceException("Invalid JWT token", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * Download the public key from the OIDC server.
     */
    private RSAPublicKey fetchPublicKey() {
        try {
            // Ottieni il documento di configurazione OIDC
            Map<String, Object> oidcConfig = restTemplate.getForObject(issuerUri + "/.well-known/openid-configuration", Map.class);
            if (oidcConfig == null) {
                throw new RuntimeException("Failed to fetch OIDC configuration");
            }
            String jwksUri = (String) oidcConfig.get("jwks_uri");

            // Obtains the JSON Web Key Set (JWKS) from the OIDC server
            Map<String, Object> jwks = restTemplate.getForObject(jwksUri, Map.class);
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");

            // Get the first key
            Map<String, Object> key = keys.get(0);
            String modulusBase64 = (String) key.get("n");
            String exponentBase64 = (String) key.get("e");

            // Convert the base64-encoded modulus and exponent to RSA public key
            byte[] modulusBytes = java.util.Base64.getUrlDecoder().decode(modulusBase64);
            byte[] exponentBytes = java.util.Base64.getUrlDecoder().decode(exponentBase64);

            java.math.BigInteger modulus = new java.math.BigInteger(1, modulusBytes);
            java.math.BigInteger exponent = new java.math.BigInteger(1, exponentBytes);

            return (RSAPublicKey) java.security.KeyFactory.getInstance("RSA")
                    .generatePublic(new java.security.spec.RSAPublicKeySpec(modulus, exponent));

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch or parse public key from OIDC provider", e);
        }
    }
}


