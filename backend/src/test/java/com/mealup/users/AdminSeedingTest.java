package com.mealup.users;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mealup.support.AbstractMySqlIntegrationTest;

/** The application starts on an empty database and creates the admin from configuration. */
class AdminSeedingTest extends AbstractMySqlIntegrationTest {

    @Autowired
    UserRepository users;

    @Autowired
    AdminSeeder seeder;

    @Test
    void adminIsCreatedOnFirstStartWithAHashedPassword() {
        User admin = users.findByEmailIgnoreCase("founder@test.mealup").orElseThrow();

        assertThat(admin.isActive()).isTrue();
        assertThat(admin.getPasswordHash()).startsWith("$2").doesNotContain("Test-Founder-Pass-1");
    }

    @Test
    void runningTheSeedAgainDoesNotCreateAnotherOne() {
        long before = users.count();

        assertThat(seeder.seed()).isFalse();
        assertThat(users.count()).isEqualTo(before);
    }
}
