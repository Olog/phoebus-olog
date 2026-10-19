# Authentication

In general, non-GET methods are protected, i.e. client needs to send a basic authentication header for each request.

The service supports four authentication schemes:

In-memory
: With hard coded username/password, see this [Java source code file]. This scheme should be used only for evaluation
  or demonstration purposes.

Embedded LDAP
: With a custom ldif file one may use this in cases where authentication with a production user directory
  is not need or even feasible. Settings for the embedded LDAP server must be defined in a file identified by the
  system property `embeddedLdapPropertySource`, see also file `embedded_ldap.properties` maintained in the project source.

LDAP
: Authentication with a remote LDAP service. Settings needed to interact with the service must be defined in a file identified by the
  system property `ldapPropertySource`, see also file `ldap.properties` maintained in the project source.

Active Directory
: Authentication with a remote Active Directory service. Settings needed to interact with the service must be defined in a file identified by the
  system property `activeDirectoryPropertySource`, see also file `active_directory.properties` maintained in the project source.

By default the authentication scheme is in-memory. To use another scheme, the system property `authenticationProviders`
can define a different one, or even multiple schemes. These are identified as `inMemory`, `embeddedLdap`, `ldap` and
`active_directory`.
Thus when launching the service one needs to add to the Java command line like so:
`-DauthenticationProviders=ldap`
To use multiple authentication providers, specify comma separated list, e.g.;
`-DauthenticationProviders=ldap,activeDirectory`. Authentication will
be attempted in the listed order, i.e. if the LDAP provider failes to 
authenticate, the authentication process proceeds to attempt Active Directory

## OpenID Connect (single sign-on)

Browser clients can log in through an OpenID Connect (OIDC) identity provider, e.g. Keycloak, Microsoft Entra ID
or AD FS. This is not an authentication scheme on its own: it complements one of the schemes above, which is still
needed for clients that authenticate with username and password (e.g. the Phoebus client or scripts using basic
authentication).

To enable it, add `oidc` to `authenticationProviders`, e.g. `-DauthenticationProviders=ldap,oidc`, and register the
identity provider with the standard Spring Boot properties. The login flow starts at
`/Olog/oauth2/authorization/<registration id>`, the identity provider redirects back to
`/Olog/login/oauth2/code/<registration id>`, which must be registered as redirect URI with the identity provider.
On success, the service creates the same session (cookie `SESSION`) as for a username/password login, so log out,
`/Olog/user` and the role checks work the same way. Afterwards the browser is redirected to the web client.

```properties
authenticationProviders=ldap,oidc

spring.security.oauth2.client.provider.keycloak.issuer-uri=https://idp.example.org/realms/example
spring.security.oauth2.client.provider.keycloak.user-name-attribute=preferred_username
spring.security.oauth2.client.registration.keycloak.client-name=Keycloak
spring.security.oauth2.client.registration.keycloak.client-id=olog
spring.security.oauth2.client.registration.keycloak.client-secret=${OIDC_CLIENT_SECRET}
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/Olog/login/oauth2/code/{registrationId}
spring.security.oauth2.client.registration.keycloak.scope=openid
```

The service reads user name and groups from the ID token:

| Property | Default | Description |
|----------|---------|-------------|
| `oidc.username-claim` | `preferred_username` | Claim holding the user name. Use the same value as the other authentication scheme (e.g. the LDAP `uid` or `sAMAccountName`), otherwise one person shows up as different log entry owners depending on how they logged in. |
| `oidc.groups-claim` | `groups` | Claim holding the group names, a string or an array. Domain prefixes (`DOMAIN\group`) are removed. Groups are mapped to roles like the LDAP groups, i.e. group `olog-admins` matches `admin-groups=olog-admins`. |
| `oidc.groups-filter` | (empty) | Optional regular expression. Only matching groups are mapped to roles, e.g. `(?i)olog-.*`. |
| `oidc.browser-password-login` | `true` | Set to `false` to ask browser clients to show only the single sign-on buttons. Username/password authentication keeps working for other clients. Ignored if no identity provider is configured. |
| `oidc.post-login-redirect-uri` | `/` | Where the browser is sent after a successful login, i.e. the web client. |
| `oidc.login-failure-redirect-uri` | `/?sso-error` | Where the browser is sent if the login fails. The reason is logged by the service. |

Clients learn about the configured identity providers from the service configuration endpoint `/Olog/configuration`:

```json
"authentication": {"passwordLogin": true, "oidcProviders": [{"id": "keycloak", "name": "Keycloak"}]}
```

Notes:

* The service must know its external URL to build the redirect URI. Behind a reverse proxy, set
  `server.forward-headers-strategy=native` (or `framework`) and let the proxy send `X-Forwarded-Proto` and
  `X-Forwarded-Host` (or the original `Host` header).
* If the identity provider uses a certificate signed by an internal certificate authority, it must be trusted by the
  Java runtime of the service.
* AD FS: give the application the scopes `openid` and `allatclaims` and issue the user name and group claims through
  issuance transform rules. Do not request the `profile` scope: AD FS would then be queried through its userinfo
  endpoint, which does not accept the access token issued for the application.

  [Java source code file]: https://github.com/Olog/phoebus-olog/blob/master/src/main/java/org/phoebus/olog/security/InMemorySecurityConfig.java

