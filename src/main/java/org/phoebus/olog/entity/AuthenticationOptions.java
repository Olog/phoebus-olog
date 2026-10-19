/*
 * Copyright (C) 2026 The Olog contributors.
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
package org.phoebus.olog.entity;

import java.util.List;

/**
 * Tells clients how users can log in, e.g. so that a browser client can render
 * single sign-on buttons instead of (or in addition to) a username/password form.
 *
 * @param passwordLogin Hint for browser clients whether to offer a username/password form. Always
 *                      <code>true</code> if no OIDC provider is configured. The service itself keeps
 *                      accepting username/password (e.g. Basic Auth) regardless of this hint.
 * @param oidcProviders Configured OpenID Connect providers, empty if OIDC login is not enabled.
 */
public record AuthenticationOptions(boolean passwordLogin, List<OidcProvider> oidcProviders) {

    public static AuthenticationOptions passwordOnly() {
        return new AuthenticationOptions(true, List.of());
    }

    /**
     * @param id   Registration id. The login flow starts at <code>&lt;service&gt;/oauth2/authorization/&lt;id&gt;</code>.
     * @param name Display name, taken from <code>spring.security.oauth2.client.registration.&lt;id&gt;.client-name</code>.
     */
    public record OidcProvider(String id, String name) {
    }
}
