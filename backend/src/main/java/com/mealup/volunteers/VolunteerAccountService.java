package com.mealup.volunteers;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.access.AccessPolicy;
import com.mealup.access.AuthenticatedUser;
import com.mealup.common.exception.BadRequestException;
import com.mealup.common.exception.ConflictException;
import com.mealup.common.exception.NotFoundException;
import com.mealup.common.exception.UnauthorizedException;
import com.mealup.common.validation.Phones;
import com.mealup.users.User;
import com.mealup.users.UserRepository;
import com.mealup.volunteers.VolunteerDtos.ChangePasswordRequest;
import com.mealup.volunteers.VolunteerDtos.MySummary;
import com.mealup.volunteers.VolunteerDtos.RegisterRequest;

/** Volunteer sign-up, a volunteer's own totals, and changing their own password. */
@Service
public class VolunteerAccountService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final VolunteerStats stats;
    private final AccessPolicy access;

    public VolunteerAccountService(UserRepository users, PasswordEncoder encoder, VolunteerStats stats, AccessPolicy access) {
        this.users = users;
        this.encoder = encoder;
        this.stats = stats;
        this.access = access;
    }

    /** Creates a volunteer account. Anyone may sign up; a volunteer can never become an admin this way. */
    @Transactional
    public AuthenticatedUser register(RegisterRequest req) {
        if (req.website() != null && !req.website().isBlank()) {
            throw new BadRequestException("The account could not be created.");     // bot
        }
        String email = req.email().trim().toLowerCase();
        String phone = Phones.normalise(req.phone());
        if (!Phones.isValid(phone)) {
            throw new BadRequestException("Enter a valid phone number (10 to 15 digits, optional leading +).");
        }
        if (req.password().equalsIgnoreCase(email)) {
            throw new BadRequestException("Choose a password that is different from your email.");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists. Please sign in.");
        }
        if (users.existsByPhoneNumber(phone)) {
            throw new ConflictException("This phone number is already used by another account.");
        }
        User saved = users.saveAndFlush(User.volunteer(req.name().trim(), phone, email, encoder.encode(req.password())));
        return AuthenticatedUser.from(saved);
    }

    @Transactional(readOnly = true)
    public MySummary mySummary() {
        AuthenticatedUser me = access.requireVolunteer();
        return stats.mine(me.id());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        AuthenticatedUser me = access.requireAccountHolder();
        User user = users.findById(me.id()).orElseThrow(() -> new NotFoundException("Account not found."));
        if (user.getPasswordHash() == null || !encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("The current password is not correct.");
        }
        if (req.newPassword().equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("Choose a password that is different from your email.");
        }
        user.setPasswordHash(encoder.encode(req.newPassword()));
        users.save(user);
    }
}
