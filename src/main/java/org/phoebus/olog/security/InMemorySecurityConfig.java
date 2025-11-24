/*
 * Copyright 2025 European Spallation Source ERIC.
 *
 */

package org.phoebus.olog.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Security config for in-memory authentication/authorization. Instantiated only if runtime property
 * <code>authenticationManager</code> is set to <code>inMemory</code>.
 */
@SuppressWarnings("unused")
@EnableWebSecurity
@Configuration
@ConditionalOnProperty(value = "authenticationManager", havingValue = "inMemory")
public class InMemorySecurityConfig extends WebSecurityConfig {

    @Bean
    public AuthenticationManager inMemoryAuthenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder().encode("userPass"))
                .roles("USER")
                .build();
        UserDetails adminUser = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("adminPass"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user, adminUser);
    }
}