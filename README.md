# Olog   [![Build Java Binary and Run Tests](https://github.com/Olog/phoebus-olog/actions/workflows/java-build-and-test.yml/badge.svg)](https://github.com/Olog/phoebus-olog/actions/workflows/java-build-and-test.yml)

An online logbook service that allows for the creation and retrieval of log entries.

[Phoebus Olog Documentation](https://olog.readthedocs.io/)

## Building and installing

For how to build and install the Phoebus Olog service,
see the [system administrator guides](https://olog.readthedocs.io/en/latest/sysadmin/guides/index.html).

## Contributing

For how to contribute to this project,
see the [contributor guides](https://olog.readthedocs.io/en/latest/contributor/guides/index.html).

## INFN extensions

### INFN Identity client

The INFN Identity Java client is generated at build time with OpenAPI Generator from
`src/main/openapi/infn-identity-v3.json`. Generated sources are written under `target/generated-sources/openapi`
and must not be edited or committed. Run `mvn generate-sources` to regenerate them.

The generated `IdentityApi` can be created for a user bearer token through
`InfnIdentityClientFactory`. The API base URL defaults to `https://api.app.infn.it` and can be overridden with
the `INFN_IDENTITY_BASE_URL` environment variable.

### Logbook collaborators

The authenticated creator of a logbook becomes its administrator. An administrator can manage collaborators with:

* `GET /Olog/logbooks/{name}/members`
* `POST /Olog/logbooks/{name}/members`
* `PUT /Olog/logbooks/{name}/members/{infnUuid}`
* `DELETE /Olog/logbooks/{name}/members/{infnUuid}`
* `GET /Olog/logbooks/{name}/permissions/me`
* `GET /Olog/logbooks/permissions/me`

The request payload accepts an INFN UUID and one or both collaborator permissions:

```json
{
  "infnUuid": "123e4567-e89b-12d3-a456-426614174000",
  "permissions": ["READ", "WRITE"],
  "visibleFrom": "2026-09-01",
  "visibleTo": "2026-09-30"
}
```

The `ADMIN` permission is reserved for the logbook creator and cannot be delegated through the collaborators API.
`visibleFrom` and `visibleTo` are optional ISO calendar dates (`YYYY-MM-DD`). They restrict
the **creation date of entries**, not the dates on which a collaborator may sign in.
Both days are inclusive in UTC; either end may be omitted. Missing or null dates mean
no limit on that side. Sending both as null on PUT removes the restriction. PUT replaces
the collaborator's permissions and period, so clients must resend dates they want to retain.
Invalid dates and reversed intervals return 400. Existing memberships without these
fields remain unrestricted in time; owners and ADMIN memberships always have full history.

Log reads now require authentication (401 for anonymous requests). A collaborator with
READ or WRITE can view entries within their period. Direct access outside the period
returns 403; search, group queries, RSS and archived versions are filtered before pagination
and counting. Attachments require access to a referencing current entry. For entries in
multiple logbooks, any valid grant permits reading the entry. Global roles/groups do not
replace a logbook grant. Updating an existing entry also requires visibility of that entry.
Clients must send credentials for image/download requests too; the web client uses
authenticated requests and temporary blob URLs for attachments.

The membership mapping adds nullable date fields; no rewrite of old documents is needed.
New indices use `logbook_member_mapping.json`; existing default dynamic mappings accept
the ISO date values. For installations with dynamic mapping disabled, add `visibleFrom`
and `visibleTo` as `date` fields (`strict_date`) before using the feature.
Creating or editing a log entry requires `WRITE` (or `ADMIN`) on every associated logbook.
OAuth deployments must configure `OAUTH2_CLAIMS_NAME` with the JWT claim containing the INFN UUID;
the default is `preferred_username`. Demo basic authentication is disabled by default and can be enabled with
`DEMO_AUTH_ENABLED=true` for local development.
