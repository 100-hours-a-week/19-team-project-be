package org.refit.refitbackend.domain.auth.jwt;

import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.entity.enums.Role;
import org.refit.refitbackend.domain.user.entity.enums.UserType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final Role role;
    private final User user;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.role = user.getRole();
        this.user = user;
    }

    public CustomUserDetails(Long userId) {
        this.userId = userId;
        this.role = Role.USER;
        this.user = null;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return user != null ? user.getEmail() : null;
    }

    public String getNickname() {
        return user != null ? user.getNickname() : null;
    }

    public String getUserType() {
        return user != null ? user.getUserType().name() : UserType.JOB_SEEKER.name();
    }

    public String getProfileImageUrl() {
        return user != null ? user.getProfileImageUrl() : null;
    }

    public String getOauthProvider() {
        return user != null ? user.getOauthProvider().name() : null;
    }

    public String getOauthId() {
        return user != null ? user.getOauthId() : null;
    }

    public Role getRole() {
        return role;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return userId != null ? userId.toString() : "";
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
        return true;
    }
}
