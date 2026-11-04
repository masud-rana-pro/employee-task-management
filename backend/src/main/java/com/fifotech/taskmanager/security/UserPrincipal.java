package com.fifotech.taskmanager.security;

import com.fifotech.taskmanager.entity.User;
import com.fifotech.taskmanager.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The authenticated user as Spring Security sees it.
 * Controllers receive it with {@code @AuthenticationPrincipal}.
 * It is a plain snapshot (no JPA entity), so it is safe to use outside a transaction.
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String fullName;
    private final Role role;
    private final boolean active;

    public UserPrincipal(Long id, String email, String password, String fullName, Role role, boolean active) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPassword(),
                user.getFullName(), user.getRole(), user.isActive());
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    /** "ROLE_ADMIN" etc. - the "ROLE_" prefix is what hasRole('ADMIN') expects. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    /** The login name is the email address. */
    @Override
    public String getUsername() {
        return email;
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

    /** Soft-deleted users (active = false) can neither log in nor use an old token. */
    @Override
    public boolean isEnabled() {
        return active;
    }
}
