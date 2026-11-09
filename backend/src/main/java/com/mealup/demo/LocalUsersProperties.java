package com.mealup.demo;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.mealup.users.Role;

/** Personal logins for this computer, read from the git-ignored backend/application-local.yml. Empty everywhere else. */
@ConfigurationProperties(prefix = "mealup.local")
public record LocalUsersProperties(List<Account> users) {

    public record Account(String name, String email, String phone, String password, Role role) { }

    public List<Account> usersOrEmpty() {
        return users == null ? List.of() : users;
    }
}
