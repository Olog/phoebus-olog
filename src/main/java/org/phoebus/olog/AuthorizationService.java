package org.phoebus.olog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;


/**
 * A service for authentication and authorization of users
 * @author kunal
 *
 */
@Service
public class AuthorizationService {

    List<String> admin_groups;
    List<String> log_groups;
    List<String> tag_groups;
    List<String> logbook_groups;
    List<String> property_groups;

    @Value("${admin-groups:olog-admins}")
    void initializeAdminRoles(String groups)
    {
        this.admin_groups = Arrays.asList(groups.split(",")).stream().map(g -> {
            return "ROLE_" + g.trim().toUpperCase();
        }).collect(Collectors.toList());
    }

    @Value("${channel-groups:olog-logs}")
    void initializeChannelModRoles(String groups)
    {
        this.log_groups = Arrays.asList(groups.split(",")).stream().map(g -> {
            return "ROLE_" + g.trim().toUpperCase();
        }).collect(Collectors.toList());
    }

    @Value("${tag-groups:olog-tags}")
    void initializeTagRoles(String groups)
    {
        this.tag_groups = Arrays.asList(groups.split(",")).stream().map(g -> {
            return "ROLE_" + g.trim().toUpperCase();
        }).collect(Collectors.toList());
    }

    @Value("${property-groups:olog-logbooks}")
    void initializeLogbookRoles(String groups)
    {
        this.logbook_groups = Arrays.asList(groups.split(",")).stream().map(g -> {
            return "ROLE_" + g.trim().toUpperCase();
        }).collect(Collectors.toList());
    }

    @Value("${property-groups:olog-properties}")
    void initializePropertyRoles(String groups)
    {
        this.property_groups = Arrays.asList(groups.split(",")).stream().map(g -> {
            return "ROLE_" + g.trim().toUpperCase();
        }).collect(Collectors.toList());
    }

    public enum ROLES
    {
        OLOG_ADMIN, OLOG_LOG, OLOG_TAG, OLOG_LOGBOOK, OLOG_PROPERTY;
    };

    /**
     * Check if the user is authorized
     * @param authentication the authentication information of the user
     * @param expectedRole the expected role
     * @return true if the user has the expected authorization role
     */
    public boolean isAuthorizedRole(Authentication authentication, ROLES expectedRole) {
        ArrayList<String> auth = new ArrayList<String>();
        Collection<? extends GrantedAuthority> auths = authentication.getAuthorities();
        for(GrantedAuthority a: auths)
            auth.add(a.getAuthority());

        if (!Collections.disjoint(auth, admin_groups))
            return true;
        switch (expectedRole) {
        case OLOG_LOG:
            if(!Collections.disjoint(auth,log_groups) ||
               !Collections.disjoint(auth,logbook_groups) ||
               !Collections.disjoint(auth,tag_groups) ||
               !Collections.disjoint(auth,property_groups)) {
                return true;
            }
            return false;
        case OLOG_LOGBOOK:
            if (!Collections.disjoint(auth, logbook_groups))
            {
                return true;
            }
            return false;
        case OLOG_TAG:
            if (!Collections.disjoint(auth, tag_groups))
            {
                return true;
            }
            return false;
        case OLOG_PROPERTY:
            if (!Collections.disjoint(auth, property_groups))
            {
                return true;
            }
            return false;
        default:
            return false;
        }
    }
}
