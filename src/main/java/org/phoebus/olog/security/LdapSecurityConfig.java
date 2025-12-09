/*
 * Copyright 2025 European Spallation Source ERIC.
 *
 */

package org.phoebus.olog.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.ldap.core.support.DefaultTlsDirContextAuthenticationStrategy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.ldap.LdapBindAuthenticationManagerFactory;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

/**
 * Security config for LDAP authentication/authorization. Instantiated only if runtime property
 * <code>authenticationManager</code> is set to <code>ldap</code>.
 */
@SuppressWarnings("unused")
@EnableWebSecurity
@Configuration
@ConditionalOnProperty(value = "authenticationManager", havingValue = "ldap")
@PropertySource("${propertySource:classpath:/security/ldap.properties}")
public class LdapSecurityConfig extends WebSecurityConfig {

    @Value("${ldap.enabled:false}")
    boolean ldap_enabled;
    @Value("${ldap.starttls:false}")
    boolean ldap_starttls;
    @Value("${ldap.urls:ldaps://localhost:389/}")
    String ldap_url;
    @Value("${ldap.base.dn}")
    String ldap_base_dn;
    @Value("${ldap.user.dn.pattern}")
    String ldap_user_dn_pattern;
    @Value("${ldap.groups.search.base}")
    String ldap_groups_search_base;
    @Value("${ldap.groups.search.pattern}")
    String ldap_groups_search_pattern;
    @Value("${ldap.manager.dn}")
    String ldap_manager_dn;
    @Value("${ldap.manager.password}")
    String ldap_manager_password;
    @Value("${ldap.user.search.base}")
    String ldap_user_search_base;
    @Value("${ldap.user.search.filter}")
    String ldap_user_search_filter;

    @Bean
    public AuthenticationManager authenticationManager(DefaultSpringSecurityContextSource contextSource) {
        LdapBindAuthenticationManagerFactory factory = new LdapBindAuthenticationManagerFactory(
                contextSource);
        if (ldap_user_dn_pattern != null && !ldap_user_dn_pattern.isEmpty()) {
            factory.setUserDnPatterns(ldap_user_dn_pattern);
        }
        if (ldap_user_search_filter != null && !ldap_user_search_filter.isEmpty()) {
            factory.setUserSearchFilter(ldap_user_search_filter);
        }
        if (ldap_user_search_base != null && !ldap_user_search_base.isEmpty()) {
            factory.setUserSearchBase(ldap_user_search_base);
        }
        factory.setLdapAuthoritiesPopulator(ldapAuthoritiesPopulator(contextSource));

        return factory.createAuthenticationManager();
    }

    @Bean
    public LdapAuthoritiesPopulator ldapAuthoritiesPopulator(DefaultSpringSecurityContextSource contextSource) {
        DefaultLdapAuthoritiesPopulator myAuthPopulator =
                new DefaultLdapAuthoritiesPopulator(contextSource, ldap_groups_search_base);
        myAuthPopulator.setGroupSearchFilter(ldap_groups_search_pattern);
        myAuthPopulator.setSearchSubtree(true);
        myAuthPopulator.setIgnorePartialResultException(true);

        return myAuthPopulator;
    }

    @Bean
    public DefaultSpringSecurityContextSource defaultSpringSecurityContextSource() {
        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldap_url);
        if (ldap_manager_dn != null && !ldap_manager_dn.isEmpty() && ldap_manager_password != null && !ldap_manager_password.isEmpty()) {
            contextSource.setUserDn(ldap_manager_dn);
            contextSource.setPassword(ldap_manager_password);
        }
        if (ldap_starttls) {
            contextSource.setAuthenticationStrategy(new DefaultTlsDirContextAuthenticationStrategy());
        }
        contextSource.afterPropertiesSet();

        return contextSource;
    }
}
