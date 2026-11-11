/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 */

package org.phoebus.olog.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.stereotype.Service;

/**
 * {@link Service} class implementing domain specific authorization rules in order to
 * grant or deny POST/PUT access to metadata endpoints.
 */
@Service("authorizationHelper")
@SuppressWarnings("unused")
public class AuthorizationHelper {

    @Autowired
    private String roleAdmin;

    @Value("${authorization.permitall:true}")
    private boolean permitAll;

    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Helper method to determine if an authenticated user may manage Olog metadata.
     * @param methodSecurityExpressionOperations Spring injected {@link MethodSecurityExpressionOperations}
     * @return <code>true</code> if {@link org.springframework.stereotype.Controller} method may be called by
     * the authenticated user.
     */
    public boolean mayManageMetadata(MethodSecurityExpressionOperations methodSecurityExpressionOperations){
        return permitAll || methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleAdmin);
    }
}
