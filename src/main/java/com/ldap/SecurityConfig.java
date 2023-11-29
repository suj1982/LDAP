package com.ldap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {


    private SecurityConfig ctx;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/user").authenticated() // Restrict access to /user
                .and()
                .formLogin()
                .and()
                .httpBasic();
    }

    @RestController
    public static class UserController {

        @GetMapping("/user")
        public ResponseEntity<Map<String, Object>> getUserDetails(@AuthenticationPrincipal CustomLdapUserDetails userDetails) {
            Map<String, Object> attributes = userDetails.getAttributes();
            return ResponseEntity.ok(attributes);
        }

    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(ldapAuthenticationProvider());
    }

    @Bean
    @Lazy
    public AuthenticationProvider ldapAuthenticationProvider() {
        DefaultSpringSecurityContextSource contextSource = ldapContext();

        BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource);
        bindAuthenticator.setUserDnPatterns(new String[]{"cn=Administrator,cn=users,dc=siteminderps,dc=exconnetworks,dc=com"});

        LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator);
        ldapAuthenticationProvider.setUserDetailsContextMapper(userDetailsContextMapper());

        return ldapAuthenticationProvider;
    }

    @Bean
    public UserDetailsContextMapper userDetailsContextMapper() {
        return new CustomUserDetailsContextMapper();
    }

    @Bean
    public DefaultSpringSecurityContextSource ldapContext() {
        return new DefaultSpringSecurityContextSource("ldap://192.168.1.23:389/");
    }

    @Bean
    public LdapShaPasswordEncoder ldapShaPasswordEncoder() {
        return new LdapShaPasswordEncoder();
    }

    public class CustomUserDetailsContextMapper implements UserDetailsContextMapper {

        @Override
        public LdapUserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {
            return new CustomLdapUserDetails(ctx, username, authorities);
        }

        @Override
        public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
            // Implement if needed
        }
    }


    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        // Implement if needed
    }

    public class CustomLdapUserDetails implements LdapUserDetails {

        private final DirContextOperations ctx;
        private final String username;
        private final Collection<? extends GrantedAuthority> authorities;

        public CustomLdapUserDetails(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {

            this.ctx = ctx;
            this.username = username;
            this.authorities = authorities;
        }


        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return false;
        }

        @Override
        public boolean isAccountNonLocked() {
            return false;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        // Additional method to get LDAP attributes

        public Map<String, Object> getAttributes() {
            Map<String, Object> attributes = new HashMap<>();
            if (ctx instanceof DirContextOperations) {
                DirContextOperations dirContextOperations = (DirContextOperations) ctx;
                Attributes ldapAttributes = dirContextOperations.getAttributes();
                NamingEnumeration<? extends Attribute> attributeEnum = ldapAttributes.getAll();
                try {
                    while (attributeEnum.hasMore()) {
                        Attribute attribute = attributeEnum.next();
                        String attributeName = attribute.getID();
                        Object attributeValue = attribute.get();
                        attributes.put(attributeName, attributeValue);
                    }
                } catch (NamingException e) {
                    // Handle the exception if needed
                    e.printStackTrace();
                }
            }
            return attributes;
        }


        @Override
        public String getDn() {
            return null;
        }

        @Override
        public void eraseCredentials() {
            // Implement if needed
        }
    }

    // Implement other UserDetails methods

    // Additional method to get LDAP attributes
    public Attributes getAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        try {
            Attributes ldapAttributes = ctx.getAttributes();
            NamingEnumeration<? extends Attribute> attributeEnum = ldapAttributes.getAll();
            while (attributeEnum.hasMore()) {
                Attribute attribute = attributeEnum.next();
                String attributeName = attribute.getID();
                Object attributeValue = attribute.get();
                attributes.put(attributeName, attributeValue);
            }
        } catch (NamingException e) {
            // Handle the exception if needed
            e.printStackTrace();
        }
        return (Attributes) attributes;
    }
}











