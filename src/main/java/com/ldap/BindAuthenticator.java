
package com.ldap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControl;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControlExtractor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticator;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControl;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControlExtractor;
import org.springframework.util.StringUtils;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.support.LdapUtils;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;


import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;


public class BindAuthenticator extends AbstractLdapAuthenticator {
    // ~ Static fields/initializers
    // =====================================================================================

    private static final Log logger = LogFactory.getLog(BindAuthenticator.class);

    // ~ Constructors
    // ===================================================================================================

    /**
     * Create an initialized instance using the {@link BaseLdapPathContextSource}
     * provided.
     *
     * @param contextSource the BaseLdapPathContextSource instance against which bind
     * operations will be performed.
     *
     */
    public BindAuthenticator(BaseLdapPathContextSource contextSource) {
        super(contextSource);
    }

    // ~ Methods
    // ========================================================================================================

    public DirContextOperations authenticate(Authentication authentication) {
        DirContextOperations user = null;
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
                "Can only process UsernamePasswordAuthenticationToken objects");

        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        if (!StringUtils.hasLength(password)) {
            logger.debug("Rejecting empty password for user " + username);
            throw new BadCredentialsException(
                    messages.getMessage("BindAuthenticator.emptyPassword", "Empty Password"));
        }

        // If DN patterns are configured, try authenticating with them directly
        for (String dn : getUserDns(username)) {
            user = bindWithDn(dn, username, password);

            if (user != null) {
                break;
            }
        }

        // Otherwise use the configured search object to find the user and authenticate
        // with the returned DN.
        if (user == null && getUserSearch() != null) {
            DirContextOperations userFromSearch = getUserSearch().searchForUser(username);
            user = bindWithDn(userFromSearch.getDn().toString(), username, password,
                    userFromSearch.getAttributes());
        }

        if (user == null) {
            throw new BadCredentialsException(
                    messages.getMessage("BindAuthenticator.badCredentials", "Bad credentials"));
        }

        return user;
    }

    private DirContextOperations bindWithDn(String userDnStr, String username, String password) {
        return bindWithDn(userDnStr, username, password, null);
    }

    private DirContextOperations bindWithDn(String userDnStr, String username, String password, Attributes attrs) {
        BaseLdapPathContextSource ctxSource = (BaseLdapPathContextSource) getContextSource();
        DistinguishedName userDn = new DistinguishedName(userDnStr);
        DistinguishedName fullDn = new DistinguishedName(userDn);
        fullDn.prepend(ctxSource.getBaseLdapPath());

        logger.debug("Attempting to bind as " + fullDn);

        DirContext ctx = null;
        try {
            ctx = getContextSource().getContext(fullDn.toString(), password);
            // Check for password policy control
            PasswordPolicyControl ppolicy = PasswordPolicyControlExtractor.extractControl(ctx);

            logger.debug("Retrieving attributes...");
            if (attrs == null || attrs.size() == 0) {
                attrs = ctx.getAttributes(userDn, getUserAttributes());
            }

            DirContextAdapter result = new DirContextAdapter(attrs, userDn, ctxSource.getBaseLdapPath());

            if (ppolicy != null) {
                result.setAttributeValue(ppolicy.getID(), ppolicy);
            }

            return result;
        } catch (NamingException e) {
            // This will be thrown if an invalid user name is used and the method may
            // be called multiple times to try different names, so we trap the exception
            // unless a subclass wishes to implement more specialized behaviour.
            if ((e instanceof org.springframework.ldap.AuthenticationException)
                    || (e instanceof org.springframework.ldap.OperationNotSupportedException)) {
                handleBindException(userDnStr, username, e);
            } else {
                throw e;
            }
        } catch (javax.naming.NamingException e) {
            throw LdapUtils.convertLdapException(e);
        } finally {
            LdapUtils.closeContext(ctx);
        }

        return null;
    }

    /**
     * Allows subclasses to inspect the exception thrown by an attempt to bind with a
     * particular DN. The default implementation just reports the failure to the debug
     * logger.
     */
    protected void handleBindException(String userDn, String username, Throwable cause) {
        if (logger.isDebugEnabled()) {
            logger.debug("Failed to bind as " + userDn + ": " + cause);
        }
    }
}
