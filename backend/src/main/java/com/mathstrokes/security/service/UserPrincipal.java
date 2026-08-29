package com.mathstrokes.security.service;

import java.util.Collection;
import java.util.List;

import com.mathstrokes.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The authenticated principal. Carries the database id so every service can derive ownership
 * from the security context instead of trusting an id sent by the client.
 */
public record UserPrincipal(Long id,
                            String phoneNumber,
                            String fullName,
                            String password,
                            boolean enabled,
                            List<String> roles) implements UserDetails {

    public static UserPrincipal from(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getPasswordHash(),
                user.isEnabled(),
                user.getRoles().stream().map(r -> r.getName().name()).sorted().toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(SimpleGrantedAuthority::new).map(GrantedAuthority.class::cast).toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return phoneNumber;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
