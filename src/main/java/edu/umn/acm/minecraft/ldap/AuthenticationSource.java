package edu.umn.acm.minecraft.ldap;

/**
 * User: bunny
 * Date: 2/28/12
 * Time: 4:51 PM
 */
public interface AuthenticationSource {
    boolean authenticate(String username, String password);
    String getFullName(String username);
}
