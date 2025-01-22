package org.phoebus.olog;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.phoebus.olog.security.SessionFilter;
import org.phoebus.olog.security.provider.JwtAuthenticationFilter;
import org.phoebus.olog.security.provider.JwtAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.sql.Driver;

@EnableWebSecurity
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    public static final String SESSION_COOKIE_NAME = "SESSION";
    public static final String ROLES_ATTRIBUTE_NAME = "roles";
    private static final Logger log = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Value("${spring.datasource.url:jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=TRUE}")
    private String h2Url;
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.authorizeRequests().anyRequest().authenticated();
        // enable JWT authentication
        if (oauth2_enabled) {
            http.addFilterBefore(jwtAuthenticationFilter(authenticationManager()), UsernamePasswordAuthenticationFilter.class);

        } else {
            http.addFilterBefore(new SessionFilter(authenticationManager(), sessionRepository()), UsernamePasswordAuthenticationFilter.class);
        }
        http.httpBasic();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // The below lists exceptions for authentication.
        web.ignoring().antMatchers(HttpMethod.GET, "/**");
        web.ignoring().antMatchers(HttpMethod.POST, "/**/login*");
        web.ignoring().antMatchers(HttpMethod.POST, "/**/logout");
        web.ignoring().antMatchers(HttpMethod.GET, "/**/user");
        // This is needed for CORS pre-flight
        web.ignoring().antMatchers(HttpMethod.OPTIONS, "/**");
        // h2 database console, if enabled.
        web.ignoring().requestMatchers(PathRequest.toH2Console());
    }

    /**
     * External Active Directory configuration properties
     */
    @Value("${ad.enabled:false}")
    boolean ad_enabled;
    @Value("${ad.url:ldap://localhost:389/}")
    String ad_url;
    @Value("${ad.domain}")
    String ad_domain;
    /**
     * External LDAP configuration properties
     */
    @Value("${ldap.enabled:false}")
    boolean ldap_enabled;
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

    /**
     * Embedded LDAP configuration properties
     */
    @Value("${embedded_ldap.enabled:false}")
    boolean embedded_ldap_enabled;
    @Value("${embedded_ldap.urls:ldaps://localhost:389/}")
    String embedded_ldap_url;
    @Value("${embedded_ldap.base.dn}")
    String embedded_ldap_base_dn;
    @Value("${embedded_ldap.user.dn.pattern}")
    String embedded_ldap_user_dn_pattern;
    @Value("${embedded_ldap.groups.search.base}")
    String embedded_ldap_groups_search_base;
    @Value("${embedded_ldap.groups.search.pattern}")
    String embedded_ldap_groups_search_pattern;
    /**
     * OAuth2 authorization based on JWT
     */
    @Value("${oauth2.enabled:false}")
    boolean oauth2_enabled;

    /**
     * Demo authorization based on in memory user credentials
     */
    @Value("${demo_auth.enabled:false}")
    boolean demo_auth_enabled;

    /**
     * File based authentication
     */
    @Value("${file.auth.enabled:true}")
    boolean file_enabled;

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        if (oauth2_enabled) {
            auth.authenticationProvider(jwtAuthenticationProvider());
        }
        if (ad_enabled) {
            ActiveDirectoryLdapAuthenticationProvider adProvider = new ActiveDirectoryLdapAuthenticationProvider(ad_domain, ad_url);
            adProvider.setConvertSubErrorCodesToExceptions(true);
	        adProvider.setUseAuthenticationRequestCredentials(true);
	        auth.authenticationProvider(adProvider);
        }
        if (ldap_enabled) {
            DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldap_url);
            if(ldap_manager_dn != null && !ldap_manager_dn.isEmpty() && ldap_manager_password != null && !ldap_manager_password.isEmpty()){
                contextSource.setUserDn(ldap_manager_dn);
                contextSource.setPassword(ldap_manager_password);
            }
            contextSource.afterPropertiesSet();

            DefaultLdapAuthoritiesPopulator myAuthPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, ldap_groups_search_base);
            myAuthPopulator.setGroupSearchFilter(ldap_groups_search_pattern);
            myAuthPopulator.setSearchSubtree(true);
            myAuthPopulator.setIgnorePartialResultException(true);

           LdapAuthenticationProviderConfigurer configurer = auth.ldapAuthentication()
                    .ldapAuthoritiesPopulator(myAuthPopulator);
           if(ldap_user_dn_pattern != null && !ldap_user_dn_pattern.isEmpty()){
               configurer.userDnPatterns(ldap_user_dn_pattern);
           }
           if(ldap_user_search_filter != null && !ldap_user_search_filter.isEmpty()){
               configurer.userSearchFilter(ldap_user_search_filter);
           }
           if(ldap_user_search_base != null && !ldap_user_search_base.isEmpty()){
               configurer.userSearchBase(ldap_user_search_base);
           }
           configurer.contextSource(contextSource);
        }

        if (embedded_ldap_enabled) {
            DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(embedded_ldap_url);
            contextSource.afterPropertiesSet();

            DefaultLdapAuthoritiesPopulator myAuthPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, embedded_ldap_groups_search_base);
            myAuthPopulator.setGroupSearchFilter(embedded_ldap_groups_search_pattern);
            myAuthPopulator.setSearchSubtree(true);
            myAuthPopulator.setIgnorePartialResultException(true);


            auth.ldapAuthentication()
                    .userDnPatterns(embedded_ldap_user_dn_pattern)
                    .ldapAuthoritiesPopulator(myAuthPopulator)
                    .groupSearchBase("ou=Group")
                    .contextSource(contextSource);

        }

        if (demo_auth_enabled) {
            auth.inMemoryAuthentication()
                    .withUser("admin").password(encoder().encode("adminPass")).roles("ADMIN").and()
                    .withUser("user").password(encoder().encode("userPass")).roles("USER");

        }
    }

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        try {
            return super.authenticationManager();
        } catch (Exception e) {
            return null;
        }
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

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(AuthenticationManager authenticationManager) {
        return new JwtAuthenticationFilter(authenticationManager);
    }

    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider() {
        return new JwtAuthenticationProvider();
    }

    /**
     * Needed to be able to customize H2 URL.
     */
    private class SessionRepositoryDataSourceFactory implements DataSourceFactory{
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
}
