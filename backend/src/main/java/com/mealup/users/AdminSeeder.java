package com.mealup.users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.config.MealUpProperties;

/**
 * Creates the admin account on the first start (when no account exists yet).
 * The credentials come from configuration / environment variables, never from a public form,
 * and there is no default password in production. An existing account is never changed by this class.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    static final int MIN_PASSWORD_LENGTH = 12;

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final MealUpProperties properties;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder, MealUpProperties properties) {
        this.users = users;
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed();
    }

    /** @return true if the admin was created, false if an account already existed. */
    @Transactional
    public boolean seed() {
        if (users.count() > 0) {
            return false;
        }
        MealUpProperties.Admin admin = properties.admin();
        if (admin == null || isBlank(admin.email()) || isBlank(admin.password())
                || isBlank(admin.fullName()) || isBlank(admin.phone())) {
            throw new IllegalStateException(
                    "No admin account exists yet. Set MEALUP_ADMIN_NAME, MEALUP_ADMIN_PHONE, MEALUP_ADMIN_EMAIL "
                    + "and MEALUP_ADMIN_PASSWORD, then start the application again (see README).");
        }
        if (admin.password().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "MEALUP_ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }
        users.save(new User(admin.fullName().trim(), admin.phone().trim(), admin.email().trim().toLowerCase(),
                encoder.encode(admin.password())));
        log.info("Created the admin account for {}.", admin.email().trim().toLowerCase());
        return true;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
