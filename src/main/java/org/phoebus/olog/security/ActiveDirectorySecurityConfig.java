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
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

/**
 * Security config for Active Directory authentication/authorization. Instantiated only if runtime property
 * <code>authenticationManager</code> is set to <code>activeDirectory</code>.
 */
@SuppressWarnings("unused")
@EnableWebSecurity
@Configuration
@ConditionalOnProperty(value = "authenticationManager", havingValue = "activeDirectory")
@PropertySource("${propertySource:classpath:/security/active_directory.properties}")
public class ActiveDirectorySecurityConfig extends WebSecurityConfig {

    @Value("${ad.url:ldap://localhost:389/}")
    String ad_url;
    @Value("${ad.domain}")
    String ad_domain;

    public ActiveDirectorySecurityConfig(){
        System.out.println();
    }

    @Bean
    public ActiveDirectoryLdapAuthenticationProvider activeDirectoryLdapAuthenticationProvider() {
        ActiveDirectoryLdapAuthenticationProvider adProvider = new ActiveDirectoryLdapAuthenticationProvider(ad_domain, ad_url);
        adProvider.setConvertSubErrorCodesToExceptions(true);
        adProvider.setUseAuthenticationRequestCredentials(true);

        return adProvider;
    }
}
