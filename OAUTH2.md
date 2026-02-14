# OAuth2/JWT Authentication Support

This document describes the OAuth2/JWT authentication features added to this fork of Phoebus Olog, which are not present in the upstream repository at https://github.com/Olog/phoebus-olog.git.

---

## Overview

This fork adds **OAuth2/JWT Bearer token authentication** alongside the existing authentication methods (LDAP, Active Directory, file-based, demo). This allows Olog to integrate with modern identity providers like Keycloak, Auth0, or any OIDC-compliant server.

### Key Features

- **JWT Bearer Token Support**: Clients can authenticate using `Authorization: Bearer <token>` headers
- **OIDC Discovery**: Automatic fetching of public keys from OIDC provider's `.well-known/openid-configuration` endpoint
- **RSA Signature Verification**: Validates JWT tokens using RSA public keys from the OIDC provider's JWKS endpoint
- **Flexible Claims Mapping**: Configurable username claim extraction from JWT tokens
- **Backward Compatible**: OAuth2 is disabled by default; existing authentication methods remain unchanged
- **Mixed Authentication**: OAuth2 can coexist with LDAP, AD, or other authentication providers

---

## Implementation Details

### New Components

#### 1. JwtAuthenticationProvider
**File**: `src/main/java/org/phoebus/olog/security/provider/JwtAuthenticationProvider.java`

Custom Spring Security `AuthenticationProvider` that:
- Validates JWT tokens against an OIDC issuer
- Fetches the OIDC provider's public key via `.well-known/openid-configuration` and JWKS endpoints
- Verifies JWT signatures using RSA public keys
- Extracts username from configurable JWT claims (default: `name`)
- Assigns `ROLE_USER` to authenticated users
- Provides detailed logging for debugging authentication issues

#### 2. JwtAuthenticationFilter
**File**: `src/main/java/org/phoebus/olog/security/provider/JwtAuthenticationFilter.java`

Spring Security filter that:
- Intercepts requests with `Authorization: Bearer <token>` headers
- Extracts JWT tokens from the Bearer scheme
- Delegates token validation to `JwtAuthenticationProvider`
- Sets authentication context on successful validation
- Returns HTTP 401 on invalid tokens

#### 3. Enhanced WebSecurityConfig
**File**: `src/main/java/org/phoebus/olog/WebSecurityConfig.java`

Modified to:
- Add `@Value("${oauth2.enabled:false}")` configuration flag
- Conditionally register `JwtAuthenticationProvider` when OAuth2 is enabled
- Conditionally add `JwtAuthenticationFilter` to the security filter chain before `UsernamePasswordAuthenticationFilter`
- Maintain backward compatibility with existing authentication methods

#### 4. Enhanced AuthenticationResource
**File**: `src/main/java/org/phoebus/olog/AuthenticationResource.java`

Updated `/user` endpoint to:
- Support JWT authentication for GET requests (which bypass the security filter chain)
- Try session-based authentication first (for backward compatibility)
- Fall back to JWT Bearer token validation from the `Authorization` header
- Return user data with roles extracted from JWT claims

---

## Configuration

### Required Properties

Add these properties to `application.properties` or set as environment variables:

```properties
############## OAuth2 Auth ##############
oauth2.enabled = ${OAUTH2_ENABLED:false}
oauth2.issueUri = ${OAUTH2_ISSUE_URI:https://localhost:8443/oauth2/issue}
oauth2.claimsName = ${OAUTH2_CLAIMS_NAME:name}
```

### Configuration Parameters

| Property | Environment Variable | Default | Description |
|----------|---------------------|---------|-------------|
| `oauth2.enabled` | `OAUTH2_ENABLED` | `false` | Enable/disable OAuth2 JWT authentication |
| `oauth2.issueUri` | `OAUTH2_ISSUE_URI` | `https://localhost:8443/oauth2/issue` | Base URL of the OIDC issuer (e.g., Keycloak realm URL) |
| `oauth2.claimsName` | `OAUTH2_CLAIMS_NAME` | `name` | JWT claim field to extract username from |

### Example: Keycloak Integration

For a Keycloak instance at `https://keycloak.example.com` with realm `olog-realm`:

```properties
oauth2.enabled=true
oauth2.issueUri=https://keycloak.example.com/realms/olog-realm
oauth2.claimsName=preferred_username
```

### Example: Auth0 Integration

```properties
oauth2.enabled=true
oauth2.issueUri=https://your-tenant.auth0.com/
oauth2.claimsName=email
```

---

## Usage

### Client Authentication

#### Session-Based (Traditional)
```bash
curl -X POST http://localhost:8080/Olog/login \
  -H 'Content-Type: application/json' \
  -d '{"username": "admin", "password": "adminPass"}'
```

#### JWT Bearer Token
```bash
curl -X PUT http://localhost:8080/Olog/logs \
  -H 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
    "owner": "test-owner",
    "source": "Test log entry",
    "title": "Test title",
    "level": "Info",
    "logbooks": [{"name": "operations"}]
  }'
```

#### Get User Info (JWT)
```bash
curl -X GET http://localhost:8080/Olog/user \
  -H 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...'
```

Response:
```json
{
  "userName": "john.doe",
  "roles": ["ROLE_USER"]
}
```

---

## Changes from Upstream

### New Files
- `src/main/java/org/phoebus/olog/security/provider/JwtAuthenticationProvider.java`
- `src/main/java/org/phoebus/olog/security/provider/JwtAuthenticationFilter.java`

### Modified Files
- `src/main/java/org/phoebus/olog/WebSecurityConfig.java`
  - Added OAuth2 configuration flag
  - Registered `JwtAuthenticationProvider`
  - Added `JwtAuthenticationFilter` to filter chain when OAuth2 is enabled
  
- `src/main/java/org/phoebus/olog/AuthenticationResource.java`
  - Enhanced `/user` endpoint to support JWT Bearer tokens
  - Added fallback authentication logic for GET requests
  
- `src/main/resources/application.properties`
  - Added OAuth2 configuration properties (`oauth2.enabled`, `oauth2.issueUri`, `oauth2.claimsName`)

### Dependencies
Added to `pom.xml`:
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

---

## Security Considerations

### Token Validation
- Tokens are validated using RSA public keys fetched from the OIDC provider
- Signatures are cryptographically verified using the `jjwt` library
- Token expiration (`exp` claim) is automatically checked
- Issuer (`iss` claim) is validated against the configured `oauth2.issueUri`

### Public Key Caching
- Currently, public keys are fetched on every authentication request
- For production deployments, consider implementing key caching to reduce latency
- Key rotation is supported via the JWKS endpoint

### HTTPS Requirement
- OAuth2/JWT tokens should only be transmitted over HTTPS in production
- The OIDC issuer URI should use HTTPS

### Role Mapping
- Currently, all JWT-authenticated users are assigned `ROLE_USER`
- For fine-grained authorization, consider extracting roles from JWT claims (e.g., `groups`, `roles`)
- Future enhancement: Map JWT claims to Olog authorization groups

---

## Troubleshooting

### Enable Debug Logging
```properties
logging.level.org.phoebus.olog.security.provider.JwtAuthenticationProvider=DEBUG
```

### Common Issues

#### "Failed to fetch public key"
- Ensure `oauth2.issueUri` is correct and reachable
- Check that the OIDC provider's `.well-known/openid-configuration` endpoint is accessible
- Verify network connectivity and SSL certificate trust

#### "Username claim not found in JWT token"
- Check the JWT token contents (use https://jwt.io to decode)
- Ensure `oauth2.claimsName` matches the claim in the token (e.g., `sub`, `name`, `preferred_username`, `email`)

#### "JWT signature verification failed"
- Token may be expired
- Token issuer may not match `oauth2.issueUri`
- OIDC provider's public key may have been rotated

#### Authentication falls through to LDAP/AD
- JWT token format may be invalid (not 3 dot-separated parts)
- OAuth2 may be disabled (`oauth2.enabled=false`)

---

## Future Enhancements

Potential improvements for OAuth2 support:

1. **Role Extraction from JWT**: Map JWT claims (e.g., `groups`, `roles`) to Olog authorization groups instead of hardcoded `ROLE_USER`
2. **Public Key Caching**: Implement TTL-based caching of OIDC public keys to reduce latency
3. **Token Refresh**: Support OAuth2 refresh token flow
4. **Multiple Issuers**: Support multiple OIDC providers simultaneously
5. **Audience Validation**: Validate the `aud` claim for better security
6. **Scope-Based Authorization**: Map OAuth2 scopes to Olog permissions

---

## Related Commits

Key commits implementing OAuth2/JWT support:

- `c258ba6` - fix: improve JWT auth with proper logging, SSL trust, and error handling
- `06f0991` - fix: Support JWT authentication in /user endpoint and fix CORS headers
- `2d287c7` - Merge branch 'feature/OAuth2' into development
- `4e0eea6` - restore update template api
- `0ab7641` - first implementation

---

## Testing

### Unit Tests
OAuth2 functionality should be tested with:
- Valid JWT tokens from a test OIDC provider
- Expired tokens (should fail)
- Tokens with invalid signatures (should fail)
- Tokens with missing username claims (should fail)

### Integration Testing
Test with a real OIDC provider:
1. Set up Keycloak or similar OIDC provider
2. Configure `oauth2.*` properties
3. Obtain a JWT token from the provider
4. Test API requests with `Authorization: Bearer <token>` header

---

## Contact

For questions or issues related to OAuth2 support:
- Repository: https://github.com/infn-epics/phoebus-olog
- Original upstream: https://github.com/Olog/phoebus-olog

---

*Last updated: February 2026*
