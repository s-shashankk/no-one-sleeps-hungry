package com.nosleepshungry.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nosleepshungry.model.User;
import com.nosleepshungry.repo.UserRepository;

/** Creates the founder's Super Admin account the first time the app starts. */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String name;
    private final String email;
    private final String password;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder,
                       @Value("${app.admin.name}") String name,
                       @Value("${app.admin.email}") String email,
                       @Value("${app.admin.password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;
        users.save(new User(name, email.trim().toLowerCase(), null, encoder.encode(password), User.Role.SUPER_ADMIN));
        log.warn("Created Super Admin '{}'. If you used the default password, change it before going live.", email);
    }
}
