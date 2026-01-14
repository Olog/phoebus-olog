/*
 * Copyright 2025 European Spallation Source ERIC.
 *
 */

package org.phoebus.olog.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.Arrays;

/**
 * Security config for in-memory authentication/authorization. Instantiated only if runtime property
 * <code>authenticationProviders</code> contains <code>inMemory</code>.
 */
@SuppressWarnings("unused")
@EnableWebSecurity
@Configuration
@Conditional(value = InMemorySecurityConfig.InMemoryCondition.class)
public class InMemorySecurityConfig {

    @Bean(WebSecurityConfig.IN_MEMORY)
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService());
        daoAuthenticationProvider.setPasswordEncoder(new BCryptPasswordEncoder());
        return daoAuthenticationProvider;
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder.encode("userPass"))
                .roles("USER")
                .build();
        UserDetails adminUser = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("adminPass"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user, adminUser);
    }

    public static class InMemoryCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String value = context.getEnvironment().getProperty(WebSecurityConfig.PROVIDER_LIST_PROPERTY_NAME);
            if (value == null || value.isEmpty()) {
                return true;
            }
            String[] values = value.split(",");
            return Arrays.asList(values).contains(WebSecurityConfig.IN_MEMORY);
        }
    }
}