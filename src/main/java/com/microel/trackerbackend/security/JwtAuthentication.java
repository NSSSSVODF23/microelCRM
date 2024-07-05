package com.microel.trackerbackend.security;

import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

@Data
public class JwtAuthentication implements Authentication {

    private boolean authenticated;
    private String username;
    private String firstName;
//    private Set<Role> roles;

    public static JwtAuthentication of(String username, String firstName, Boolean authenticated){
        JwtAuthentication jwtAuthentication = new JwtAuthentication();
        jwtAuthentication.setAuthenticated(authenticated);
        jwtAuthentication.setUsername(username);
        jwtAuthentication.setFirstName(firstName);
        return jwtAuthentication;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return firstName;
    }
}
