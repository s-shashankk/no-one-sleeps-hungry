package com.mealup.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mealup.config.MealUpProperties;

/** Plain unit tests (no Spring, no Docker). */
@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    UserRepository users;

    @Mock
    PasswordEncoder encoder;

    private AdminSeeder seederWith(String name, String phone, String email, String password) {
        return new AdminSeeder(users, encoder,
                new MealUpProperties(new MealUpProperties.Admin(name, phone, email, password)));
    }

    @Test
    void createsTheAdminWhenNoAccountExists() {
        when(users.count()).thenReturn(0L);
        when(encoder.encode("a-long-enough-password")).thenReturn("HASH");

        boolean created = seederWith("Founder", "+911234567890", "  Founder@Example.COM ", "a-long-enough-password").seed();

        assertThat(created).isTrue();
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("founder@example.com");
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("HASH");
    }

    @Test
    void doesNothingWhenAnAccountAlreadyExists() {
        when(users.count()).thenReturn(1L);

        assertThat(seederWith(null, null, null, null).seed()).isFalse();
        verify(users, never()).save(any());
    }

    @Test
    void refusesToStartWithoutCredentialsWhenNoAccountExists() {
        when(users.count()).thenReturn(0L);

        assertThatThrownBy(() -> seederWith("Founder", "+911234567890", "", "").seed())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MEALUP_ADMIN_PASSWORD");
        verify(users, never()).save(any());
    }

    @Test
    void refusesAShortPassword() {
        when(users.count()).thenReturn(0L);

        assertThatThrownBy(() -> seederWith("Founder", "+911234567890", "f@example.com", "short").seed())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12");
        verify(users, never()).save(any());
    }
}
