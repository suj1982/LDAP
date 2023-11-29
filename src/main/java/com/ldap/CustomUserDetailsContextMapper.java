package com.ldap;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.Collection;

public class CustomUserDetailsContextMapper implements UserDetailsContextMapper {

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                          Collection<? extends GrantedAuthority> authorities) {
        // Your mapping logic here
        // Extract attributes from DirContextOperations and create UserDetails object
        // Example:
        String email = ctx.getStringAttribute("mail");
        return new User(username, "password", authorities); // Replace with your UserDetails object
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        // Your mapping logic here when interacting with the LDAP context
        // Example:
        // ctx.setAttributeValue("mail", ((CustomUserDetails) user).getEmail());
    }
}

