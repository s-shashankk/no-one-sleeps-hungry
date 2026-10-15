package com.mealup.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.access.AuthenticatedUser;
import com.mealup.auth.AuthDtos.MeResponse;
import com.mealup.common.exception.UnauthorizedException;
import com.mealup.users.User;
import com.mealup.users.UserRepository;

@Service
public class AuthService {

    /** Same message for every failure, so the response never reveals which emails have accounts. */
    static final String BAD_LOGIN = "Incorrect email or password, or the account is not active.";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
        // Compared against when the email is unknown, so the response time does not reveal whether it exists.
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    /** Checks the credentials, records the login time and returns who logged in. */
    @Transactional
    public AuthenticatedUser login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        String hash = user != null && user.getPasswordHash() != null ? user.getPasswordHash() : dummyHash;
        boolean passwordOk = encoder.matches(password, hash);
        if (user == null || !passwordOk || user.getPasswordHash() == null || !user.isActive()) {
            throw new UnauthorizedException(BAD_LOGIN);
        }
        user.recordLogin();
        return AuthenticatedUser.from(user);
    }

    @Transactional(readOnly = true)
    public MeResponse me(AuthenticatedUser current) {
        User user = users.findById(current.id()).orElseThrow(() -> new UnauthorizedException("Authentication is required."));
        return new MeResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhoneNumber());
    }
}
