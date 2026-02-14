package org.phoebus.olog.security.provider;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
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

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationProvider.class);

    @Value("${oauth2.issueUri}")
    String issuerUri;

    @Value("${oauth2.claimsName}")
    String claimsName;
    private final RestTemplate restTemplate = new RestTemplate();


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String jwtToken = (String) authentication.getCredentials();

        // Skip if credentials don't look like a JWT token (3 dot-separated parts)
        if (jwtToken == null || jwtToken.split("\\.").length != 3) {
            return null;
        }

        try {
            log.info("Attempting JWT authentication against issuer: {}", issuerUri);
            // Check the token
            RSAPublicKey publicKey = fetchPublicKey();
            log.info("Successfully fetched public key from OIDC provider");
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(jwtToken)
                    .getBody();

            log.info("JWT signature verified successfully. Claims: iss={}, sub={}, exp={}", 
                    claims.getIssuer(), claims.getSubject(), claims.getExpiration());

            String username = claims.get(claimsName, String.class);
            if (username == null) {
                log.error("Username claim '{}' not found in JWT token. Available claims: {}", claimsName, claims.keySet());
                throw new UsernameNotFoundException("Username not found in token using claim: " + claimsName);
            }

            log.info("JWT authentication successful for user: {}", username);

            // Assign the user the ROLE_USER role
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            return new UsernamePasswordAuthenticationToken(username, jwtToken, authorities);

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("JWT authentication failed: {} - {}", e.getClass().getName(), e.getMessage(), e);
            // Use InternalAuthenticationServiceException to prevent ProviderManager
            // from falling through to other providers (e.g. DaoAuthenticationProvider)
            // which would mask the real JWT error with a generic "Bad credentials"
            throw new InternalAuthenticationServiceException("Invalid JWT token: " + e.getMessage(), e);
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
            // Get the OIDC configuration document
            String oidcUrl = issuerUri + "/.well-known/openid-configuration";
            log.debug("Fetching OIDC configuration from: {}", oidcUrl);
            Map<String, Object> oidcConfig = restTemplate.getForObject(oidcUrl, Map.class);
            if (oidcConfig == null) {
                throw new RuntimeException("Failed to fetch OIDC configuration from: " + oidcUrl);
            }
            String jwksUri = (String) oidcConfig.get("jwks_uri");
            log.debug("Fetching JWKS from: {}", jwksUri);

            // Obtains the JSON Web Key Set (JWKS) from the OIDC server
            Map<String, Object> jwks = restTemplate.getForObject(jwksUri, Map.class);
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");

            // Get the first key
            Map<String, Object> key = keys.get(0);
            String kid = (String) key.get("kid");
            log.debug("Using JWKS key with kid: {}", kid);
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
            log.error("Failed to fetch public key from OIDC provider at {}: {}", issuerUri, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch or parse public key from OIDC provider", e);
        }
    }
}


