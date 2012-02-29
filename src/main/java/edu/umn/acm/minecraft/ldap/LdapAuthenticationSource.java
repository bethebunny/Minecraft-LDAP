package edu.umn.acm.minecraft.ldap;

import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapUtils;

import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Enumeration;

/**
 * User: bunny
 * Date: 2/28/12
 * Time: 4:21 PM
 */
public class LdapAuthenticationSource implements AuthenticationSource {
    private static SearchControls SEARCH_CONTROLS = new SearchControls();
    static {SEARCH_CONTROLS.setSearchScope(SearchControls.SUBTREE_SCOPE);}
    
    private LdapContextSource contextSource;
    private String userIdFieldName;
    private String userFullnameFieldName;
    private String userAccountObjectClass;

    public LdapAuthenticationSource() {}

    public LdapAuthenticationSource(String url, String ldapBase, String userIdFieldName, String userAccountObjectClass) {
        this.contextSource = new LdapContextSource();
        this.userAccountObjectClass = userAccountObjectClass;
        this.userIdFieldName = userIdFieldName;
        this.setUrl(url);
        this.setBaseLdapPath(ldapBase);
        try {
            this.connect();
        } catch (Exception e) { }
    }

    public void setUrl(String url) {
        this.contextSource.setUrl(url);
    }
    
    public void setBaseLdapPath(String ldapPath) {
        this.contextSource.setBase(ldapPath);
    }
    
    public void setUserIdFieldName(String fieldName) {
        this.userIdFieldName = fieldName;
    }

    public void setUserFullnameFieldName(String fieldName) {
        this.userFullnameFieldName = fieldName;
    }

    public void setUserAccountObjectClass(String objectClass) {
        this.userAccountObjectClass = objectClass;
    }
    
    public void connect() throws Exception {
        this.contextSource.afterPropertiesSet();
    }

    private static class NoResultsException extends Exception {}
    private SearchResult getSingleRecord(String uid) throws NamingException, NoResultsException {
        String queryStr = "(&(objectClass=%s)(%s=%s))";
        queryStr = String.format(queryStr, this.userAccountObjectClass, this.userIdFieldName, uid);
        Enumeration<SearchResult> results = this.contextSource.getReadOnlyContext().search("", queryStr, SEARCH_CONTROLS);
        if (!results.hasMoreElements()) { throw new NoResultsException(); }
        return results.nextElement();
    }

    public boolean authenticate(String uid, String password) {
        DirContext context = null;
        try {
            String dn = getSingleRecord(uid).getName();
            String full_dn = String.format("%s,%s", dn, this.contextSource.getBaseLdapPathAsString());
            context = contextSource.getContext(full_dn, password);
            context.lookup(dn);
            return true;
        } catch (Exception e) {
            return false; // Failed authentication
        } finally {
            LdapUtils.closeContext(context);
        }
    }

    public String getFullName(String uid) {
        if (this.userFullnameFieldName != null) {
            try {
                return getSingleRecord(uid).getAttributes().get(this.userFullnameFieldName).get().toString();
            } catch (Exception e) { }
        }
        return null;
    }
}
