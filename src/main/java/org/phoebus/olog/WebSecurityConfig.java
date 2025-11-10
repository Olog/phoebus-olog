package org.phoebus.olog;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.ApplicationContext;
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
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.core.support.DefaultTlsDirContextAuthenticationStrategy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.ldap.EmbeddedLdapServerContextSourceFactoryBean;
import org.springframework.security.config.ldap.LdapBindAuthenticationManagerFactory;
import org.springframework.security.config.ldap.LdapPasswordComparisonAuthenticationManagerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.server.UnboundIdContainer;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.PersonContextMapper;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.sql.Driver;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
@EnableWebSecurity
@Configuration
public class WebSecurityConfig {

    public static final String SESSION_COOKIE_NAME = "SESSION";
    public static final String ROLES_ATTRIBUTE_NAME = "roles";

    @Value("${spring.datasource.url:jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=TRUE}")
    private String h2Url;

    @Value("${authenticationScheme:inMemory}")
    private String authenticationScheme;

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

    /**
     * Embedded LDAP configuration properties
     */
    //@Value("${embedded_ldap.enabled:false}")
    //boolean embedded_ldap_enabled;
    @Value("${embedded_ldap.urls:ldaps://localhost:389/}")
    String embedded_ldap_url;
    //@Value("${embedded_ldap.base.dn}")
    //String embedded_ldap_base_dn;
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

    public WebSecurityConfig(){
        System.out.println();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a
                .anyRequest()
                .authenticated())
                .csrf(c -> c.disable())
                .httpBasic(Customizer.withDefaults());
        //http.addFilterBefore(new SessionFilter(authenticationManager(http), sessionRepository()), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web
                .ignoring()
                .requestMatchers(HttpMethod.GET, "/**")
                .requestMatchers(HttpMethod.POST, "/login")
                .requestMatchers(HttpMethod.POST, "/logout")
                .requestMatchers(HttpMethod.GET, "/user")
                .requestMatchers(HttpMethod.OPTIONS, "/**")
                .requestMatchers(PathRequest.toH2Console());
    }

    @Bean
    @ConditionalOnProperty(name = "authenticationScheme", havingValue = "activeDirectory")
    public ActiveDirectoryLdapAuthenticationProvider activeDirectoryLdapAuthenticationProvider(){
        ActiveDirectoryLdapAuthenticationProvider adProvider = new ActiveDirectoryLdapAuthenticationProvider(ad_domain, ad_url);
        adProvider.setConvertSubErrorCodesToExceptions(true);
        adProvider.setUseAuthenticationRequestCredentials(true);

        return adProvider;
    }

    /*
    @Bean
    @ConditionalOnProperty(name = "authenticationScheme", havingValue = "ldap")
    public AuthenticationManager ldapAuthenticationManager(){

        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldap_url);
        if(ldap_starttls){
            contextSource.setAuthenticationStrategy(new DefaultTlsDirContextAuthenticationStrategy());
        }
        LdapPasswordComparisonAuthenticationManagerFactory factory = new LdapPasswordComparisonAuthenticationManagerFactory(
                contextSource, new BCryptPasswordEncoder());
        if(ldap_manager_dn != null && !ldap_manager_dn.isEmpty() && ldap_manager_password != null && !ldap_manager_password.isEmpty()){
            factory.setUserDnPatterns(ldap_manager_dn);
            factory.setPasswordAttribute(ldap_manager_password);
        }
        contextSource.afterPropertiesSet();
        return factory.createAuthenticationManager();
    }

     */

    /*
    @Bean
    @ConditionalOnProperty(name = "authenticationScheme", havingValue = "ldap")
    LdapAuthoritiesPopulator ldapAuthoritiesPopulator(BaseLdapPathContextSource contextSource) {
        DefaultLdapAuthoritiesPopulator myAuthPopulator =
                new DefaultLdapAuthoritiesPopulator(contextSource, ldap_groups_search_base);
        myAuthPopulator.setGroupSearchFilter(ldap_groups_search_pattern);
        myAuthPopulator.setSearchSubtree(true);
        myAuthPopulator.setIgnorePartialResultException(true);
        myAuthPopulator.setGroupSearchFilter("member={0}");

        LdapAuthenticationProviderConfigurer configurer = new LdapAuthenticationProviderConfigurer();
        configurer.ldapAuthoritiesPopulator(myAuthPopulator);
        if(ldap_user_dn_pattern != null && !ldap_user_dn_pattern.isEmpty()){
            configurer.userDnPatterns(ldap_user_dn_pattern);
        }
        if(ldap_user_search_filter != null && !ldap_user_search_filter.isEmpty()){
            configurer.userSearchFilter(ldap_user_search_filter);
        }
        if(ldap_user_search_base != null && !ldap_user_search_base.isEmpty()){
            configurer.userSearchBase(ldap_user_search_base);
        }

        return myAuthPopulator;
    }

     */

    /*
    @Bean
    @ConditionalOnProperty(name = "authenticationScheme", havingValue = "ldapEmbedded")
    public EmbeddedLdapServerContextSourceFactoryBean embeddedLdapServerContextSourceFactoryBean(){
        EmbeddedLdapServerContextSourceFactoryBean contextSourceFactoryBean =
                EmbeddedLdapServerContextSourceFactoryBean.fromEmbeddedLdapServer();
        contextSourceFactoryBean.setRoot("dc=olog,dc=local");
        return contextSourceFactoryBean;
    }

     */


    @Bean
    public AuthenticationManager authenticationManager(DefaultSpringSecurityContextSource contextSource) {
        LdapPasswordComparisonAuthenticationManagerFactory factory = new LdapPasswordComparisonAuthenticationManagerFactory(
                contextSource, new BCryptPasswordEncoder());
        factory.setUserDnPatterns("uid={0},ou=people");
        factory.setLdapAuthoritiesPopulator(defaultLdapAuthoritiesPopulator(contextSource));

        return factory.createAuthenticationManager();
    }


    @Bean
    @ConditionalOnProperty(name = "authenticationScheme", havingValue = "ldapEmbedded")
    public OlogUnboundIdContainer ldapContainer() {
        OlogUnboundIdContainer container = new OlogUnboundIdContainer("dc=olog,dc=local", "classpath:olog.ldif");
        container.setPort(8389);
        return container;
    }



    /*
    @Bean
    public AuthenticationManager test(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception{
        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(embedded_ldap_url);
        //contextSource.afterPropertiesSet();

        DefaultLdapAuthoritiesPopulator myAuthPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, embedded_ldap_groups_search_base);
        myAuthPopulator.setGroupSearchFilter(embedded_ldap_groups_search_pattern);
        myAuthPopulator.setSearchSubtree(true);
        myAuthPopulator.setIgnorePartialResultException(true);

        authenticationManagerBuilder.ldapAuthentication()
                .userDnPatterns(embedded_ldap_user_dn_pattern)
                .ldapAuthoritiesPopulator(myAuthPopulator)
                .groupSearchBase("ou=Group")
                .contextSource(contextSource);
        return authenticationManagerBuilder.getOrBuild();
    }
*/



    @Bean
    public DefaultSpringSecurityContextSource contextSource(OlogUnboundIdContainer container) {
        return new DefaultSpringSecurityContextSource("ldap://localhost:8389/dc=olog,dc=local");
    }


    /*
    @Bean
    public BindAuthenticator authenticator(BaseLdapPathContextSource contextSource) {
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserDnPatterns(new String[] { "uid={0},ou=People"});
        return authenticator;
    }
    */



    @Bean
    @ConditionalOnProperty(name = "authenticationScheme", havingValue = "ldapEmbedded")
    public DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator(ContextSource contextSource){
        DefaultLdapAuthoritiesPopulator myAuthPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, embedded_ldap_groups_search_base);
        myAuthPopulator.setGroupSearchFilter(embedded_ldap_groups_search_pattern);
        myAuthPopulator.setSearchSubtree(true);
        myAuthPopulator.setIgnorePartialResultException(true);
        myAuthPopulator.setGroupSearchFilter("ou=Group");

        return myAuthPopulator;
    }



    /*
    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {

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

     */


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /*
    @Bean
    @ConditionalOnProperty(name = "authenticationScheme", havingValue = "inMemory")
    public AuthenticationManager inMemoryAuthenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();
        return authenticationManager;
    }


     */
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

    /**
     * For inMemory authentication, suitable for demo and development.
     * @return A InMemoryUserDetailsManager
     */
    /*
    @Bean
    @ConditionalOnProperty(name = "authenticationScheme", havingValue = "inMemory")
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

     */
}
