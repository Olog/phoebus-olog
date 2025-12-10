/*
 * Copyright 2025 European Spallation Source ERIC.
 *
 */

package org.phoebus.olog.security;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.olog.OlogResourceDescriptors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.ConnectionProperties;
import org.springframework.jdbc.datasource.embedded.DataSourceFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.UrlHandlerFilter;

import javax.sql.DataSource;
import java.sql.Driver;

/**
 * Configuration for beans common for all authentication manager schemes.
 */
@SuppressWarnings("unused")
public class WebSecurityConfig {

    public static final String SESSION_COOKIE_NAME = "SESSION";
    public static final String ROLES_ATTRIBUTE_NAME = "roles";

    @Value("${spring.datasource.url:jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=TRUE}")
    private String h2Url;

    @Autowired
    private ApplicationContext context;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationManager authenticationManager = context.getBean(AuthenticationManager.class);
        http.authorizeHttpRequests(a -> a
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(new SessionFilter(authenticationManager, sessionRepository()), UsernamePasswordAuthenticationFilter.class)
                .csrf(c -> c.disable())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        PathRequest.H2ConsoleRequestMatcher s = PathRequest.toH2Console();
        return (web) -> web
                .ignoring()
                .requestMatchers(HttpMethod.GET, "/" + OlogResourceDescriptors.OLOG_SERVICE + "/**")
                .requestMatchers(HttpMethod.OPTIONS, "/" + OlogResourceDescriptors.OLOG_SERVICE + "/**")
                .requestMatchers(HttpMethod.POST, "/" + OlogResourceDescriptors.OLOG_SERVICE + "/login")
                .requestMatchers(HttpMethod.GET, "/" + OlogResourceDescriptors.OLOG_SERVICE + "/logout")
                .requestMatchers(HttpMethod.GET, "/" + OlogResourceDescriptors.OLOG_SERVICE + "/user")
                .requestMatchers(HttpMethod.GET, "/" + OlogResourceDescriptors.OLOG_SERVICE + "/h2-console/**");
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The {@link DataSource} for the session repository.
     *
     * @return A {@link DataSource} used to manage sessions.
     */
    @Bean
    @Profile("!ITtest")
    public DataSource dataSource() {
        SessionRepositoryDataSourceFactory factory = new SessionRepositoryDataSourceFactory();
        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder().addScript("schema-h2.sql");
        EmbeddedDatabase db = builder.setType(EmbeddedDatabaseType.H2)
                .setDataSourceFactory(factory)
                .build();
        return db;
    }

    /**
     * A session repository managing the sessions created when user logs in though the
     * dedicated endpoint.
     *
     * @return a {@link FindByIndexNameSessionRepository} to manage sessions.
     */
    @Bean
    @Profile("!ITtest")
    public FindByIndexNameSessionRepository sessionRepository() {
        JdbcOperations jdbcOperations = new JdbcTemplate(dataSource());
        TransactionOperations transactionOperations =
                new TransactionTemplate(new DataSourceTransactionManager(dataSource()));

        return new JdbcIndexedSessionRepository(jdbcOperations, transactionOperations);
    }

    @Bean
    @Scope("singleton")
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Bean
    @Primary
    public H2ConsoleProperties h2ConsoleProperties() {
        return new H2ConsoleProperties();
    }

    /**
     * Needed to be able to customize H2 URL.
     */
    private class SessionRepositoryDataSourceFactory implements DataSourceFactory {
        private final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();

        @Override
        public ConnectionProperties getConnectionProperties() {
            return new ConnectionProperties() {
                @Override
                public void setDriverClass(Class<? extends Driver> driverClass) {
                    dataSource.setDriverClass(driverClass);
                }

                @Override
                public void setUrl(String url) {
                    dataSource.setUrl(h2Url);
                }

                @Override
                public void setUsername(String username) {
                    dataSource.setUsername("sa");
                }

                @Override
                public void setPassword(String password) {
                    dataSource.setPassword("");
                }
            };
        }

        @Override
        public DataSource getDataSource() {
            return this.dataSource;
        }
    }

    /**
     * This bean is needed to account for requests with a trailing slash,
     * e.g. from npm development server when requesting http://localhost:8080/Olog/
     * @return A {@link FilterRegistrationBean} to handle trailing slash in a request URL
     */
    @Bean
    public FilterRegistrationBean urlHandlerFilterRegistrationBean() {
        FilterRegistrationBean<OncePerRequestFilter> registrationBean =
                new FilterRegistrationBean<>();
        UrlHandlerFilter urlHandlerFilter = UrlHandlerFilter
                .trailingSlashHandler("/Olog/**").wrapRequest()
                .build();
        registrationBean.setFilter(urlHandlerFilter);

        return registrationBean;
    }
}
