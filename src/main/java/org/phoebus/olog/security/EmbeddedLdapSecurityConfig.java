/*
 * Copyright 2025 European Spallation Source ERIC.
 *
 */

package org.phoebus.olog.security;

import org.phoebus.olog.OlogUnboundIdContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.ldap.core.ContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.ldap.LdapPasswordComparisonAuthenticationManagerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

/**
 * Security config for embedded LDAP authentication/authorization. Instantiated only if runtime property
 * <code>authenticationManager</code> is set to <code>embedded_ldap</code>.
 */
@SuppressWarnings("unused")
@EnableWebSecurity
@Configuration
@ConditionalOnProperty(value = "authenticationManager", havingValue = "embeddedLdap")
@PropertySource("${propertySource:classpath:/security/embedded_ldap.properties}")
public class EmbeddedLdapSecurityConfig extends WebSecurityConfig {

    @Value("${embedded_ldap.url:ldap://localhost:8389/dc=olog,dc=local}")
    String embedded_ldap_url;
    @Value("${embedded_ldap.base.dn}")
    String embedded_ldap_base_dn;
    @Value("${embedded_ldap.user.dn.pattern}")
    String embedded_ldap_user_dn_pattern;
    @Value("${embedded_ldap.groups.search.base}")
    String embedded_ldap_groups_search_base;
    @Value("${embedded_ldap.groups.search.pattern}")
    String embedded_ldap_groups_search_pattern;
    @Value("${spring.ldap.embedded.port:8389}")
    int spring_ldap_embedded_port;
    @Value("${embedded_ldap_ldif:classpath:olog.ldif}")
    String embedded_ldap_ldif;

    @Bean
    public AuthenticationManager authenticationManager(DefaultSpringSecurityContextSource contextSource) {
        LdapPasswordComparisonAuthenticationManagerFactory factory = new LdapPasswordComparisonAuthenticationManagerFactory(
                contextSource, new BCryptPasswordEncoder());
        factory.setUserDnPatterns(embedded_ldap_user_dn_pattern);
        factory.setLdapAuthoritiesPopulator(defaultLdapAuthoritiesPopulator(contextSource));

        return factory.createAuthenticationManager();
    }

    @Bean
    public OlogUnboundIdContainer ldapContainer() {
        OlogUnboundIdContainer container = new OlogUnboundIdContainer(embedded_ldap_base_dn, embedded_ldap_ldif);
        container.setPort(spring_ldap_embedded_port);
        return container;
    }

    @Bean
    public DefaultSpringSecurityContextSource contextSource(OlogUnboundIdContainer container) {
        return new DefaultSpringSecurityContextSource(embedded_ldap_url);
    }

    @Bean
    public DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator(ContextSource contextSource){
        DefaultLdapAuthoritiesPopulator myAuthPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, embedded_ldap_groups_search_base);
        myAuthPopulator.setGroupSearchFilter(embedded_ldap_groups_search_pattern);
        myAuthPopulator.setSearchSubtree(true);
        myAuthPopulator.setIgnorePartialResultException(true);

        return myAuthPopulator;
    }
}
