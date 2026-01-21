package org.refit.refitbackend.domain.auth.jwt;

import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.entity.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getNickname() {
        return user.getNickname();
    }

    public String getUserType() {
        return user.getUserType().name();
    }

    public String getProfileImageUrl() {
        return user.getProfileImageUrl();
    }

    public String getOauthProvider() {
        return user.getOauthProvider().name();
    }

    public String getOauthId() {
        return user.getOauthId();
    }

    public Role getRole() {
        return user.getRole();
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return user.getId() != null ? user.getId().toString() : "";
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
