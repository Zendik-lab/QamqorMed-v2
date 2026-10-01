package com.qamqormed.security;

import com.qamqormed.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public final class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String phone;
    private final String password;
    private final Role role;
    private final boolean enabled;

    public AuthenticatedUser(Long id, String phone, String password, Role role, boolean enabled) {
        this.id = id;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
