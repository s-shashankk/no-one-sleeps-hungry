package com.mealup.demo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mealup.users.Role;
import com.mealup.users.User;
import com.mealup.users.UserRepository;
import com.mealup.vendors.Vendor;
import com.mealup.vendors.VendorRepository;

/**
 * Creates the personal admin, hotel-staff and volunteer logins listed in the git-ignored backend/application-local.yml.
 * Does nothing when that file is absent (production, tests, other computers). Safe to run again: an email that already
 * exists is left alone. A hotel-staff login is linked to the sample hotel (or the first active hotel).
 */
@Component
@Order(3)                       // after the admin account (1) and the sample hotels (2)
public class LocalUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalUserSeeder.class);

    private final LocalUsersProperties props;
    private final UserRepository users;
    private final VendorRepository vendors;
    private final PasswordEncoder encoder;

    public LocalUserSeeder(LocalUsersProperties props, UserRepository users, VendorRepository vendors, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.vendors = vendors;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (LocalUsersProperties.Account a : props.usersOrEmpty()) {
            if (users.existsByEmailIgnoreCase(a.email()) || users.existsByPhoneNumber(a.phone())) continue;
            if (a.password() == null || a.password().length() < 12 || a.password().toUpperCase().contains("CHANGE-ME")) {
                log.warn("Login {} was skipped: give it a real password of 12 or more characters.", a.email());
                continue;
            }
            String hash = encoder.encode(a.password());
            Role role = a.role() == null ? Role.VOLUNTEER : a.role();
            User u;
            if (role == Role.ADMIN) {
                u = new User(a.name(), a.phone(), a.email(), hash);          // a new User is an admin by default
            } else {
                u = User.volunteer(a.name(), a.phone(), a.email(), hash);
                if (role == Role.HOTEL) {
                    Vendor hotel = pickHotel();
                    if (hotel == null) {
                        log.warn("No hotel exists yet, so {} was not created as hotel staff. Add a hotel, then restart.", a.email());
                        continue;
                    }
                    u.makeHotelStaff(hotel);
                }
            }
            users.save(u);
            log.info("Local {} login created: {}", role, a.email());
        }
    }

    private Vendor pickHotel() {
        return vendors.findFirstByNameIgnoreCase("Sunrise Residency (sample)")
                .orElseGet(() -> {
                    List<Vendor> all = vendors.findByActiveTrueOrderByNameAsc();
                    return all.isEmpty() ? null : all.get(0);
                });
    }
}
