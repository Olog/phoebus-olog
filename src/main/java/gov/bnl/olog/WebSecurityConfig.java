package gov.bnl.olog;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.bnl.olog.security.SessionFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@EnableWebSecurity
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    public static final String SESSION_COOKIE_NAME = "SESSION";
    public static final String ROLES_ATTRIBUTE_NAME = "roles";

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.authorizeRequests().anyRequest().authenticated();
        http.addFilterBefore(new SessionFilter(authenticationManager(), sessionRepository()), UsernamePasswordAuthenticationFilter.class);
        http.httpBasic();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(HttpMethod.GET, "/**");
        web.ignoring().antMatchers(HttpMethod.POST, "/login*");
        web.ignoring().antMatchers(HttpMethod.POST, "/logout*");
        web.ignoring().requestMatchers(PathRequest.toH2Console());
        // Authentication and Authorization is only needed for non search/query operations
    }

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

        if (ldap_enabled) {
            DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldap_url);
            contextSource.afterPropertiesSet();

            DefaultLdapAuthoritiesPopulator myAuthPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, ldap_groups_search_base);
            myAuthPopulator.setGroupSearchFilter(ldap_groups_search_pattern);
            myAuthPopulator.setSearchSubtree(true);
            myAuthPopulator.setIgnorePartialResultException(true);

            auth.ldapAuthentication()
                    .userDnPatterns(ldap_user_dn_pattern)
                    .ldapAuthoritiesPopulator(myAuthPopulator)
                    .contextSource(contextSource);
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
     * @return
     */
    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("org/springframework/session/jdbc/schema-h2.sql")
                .build();
    }

    /**
     * A session repository managing the sessions created when user logs in though the
     * dedicated endpoint.
     * @return
     */
    @Bean
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
    public H2ConsoleProperties h2ConsoleProperties(){
        return new H2ConsoleProperties();
    }
}