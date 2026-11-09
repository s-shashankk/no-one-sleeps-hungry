package com.mealup.access;

import java.io.Serializable;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.mealup.users.Role;
import com.mealup.users.User;

/** Who is making the current request. It is rebuilt from the database on every request (SessionUserRefreshFilter). */
public record AuthenticatedUser(Long id, String email, String fullName, Role role) implements Serializable {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
