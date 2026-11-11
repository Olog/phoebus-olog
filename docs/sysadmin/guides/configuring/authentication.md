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

By default, the authentication scheme is in-memory. To use another scheme, the system property `authenticationProviders`
can define a different one, or even multiple schemes. These are identified as `inMemory`, `embeddedLdap`, `ldap` and
`active_directory`.
Thus, when launching the service, one needs to add to the Java command line like so:
`-DauthenticationProviders=ldap`
To use multiple authentication providers, specify comma separated list, e.g.;
`-DauthenticationProviders=ldap,activeDirectory`. Authentication will
be attempted in the listed order, i.e. if the LDAP provider failes to 
authenticate, the authentication process proceeds to attempt Active Directory.

***Authorization***

By default, authenticated users may access endpoints related to management of metadata (logbooks, tags, properties,
levels and templates). To enable authorization for these endpoints one must:

* Launch the service with Spring Boot property ```authorization.permitall=false```
* Define (or use existing) user group in the accounts provider (e.g. LDAP). The name of this group defaults to ```olog-admin```,
but may be customized through a Spring Boot property, e.g. ```role.admin=<group name>```.


  [Java source code file]: https://github.com/Olog/phoebus-olog/blob/master/src/main/java/org/phoebus/olog/security/InMemorySecurityConfig.java

