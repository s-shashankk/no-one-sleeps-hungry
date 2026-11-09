package com.mealup.volunteers;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.access.AccessPolicy;
import com.mealup.common.api.PageResponse;
import com.mealup.common.exception.NotFoundException;
import com.mealup.pickups.PickupDtos.PickupResponse;
import com.mealup.pickups.PickupService;
import com.mealup.users.Role;
import com.mealup.users.User;
import com.mealup.users.UserRepository;
import com.mealup.pickups.PickupRepository;
import com.mealup.pickups.PickupStatus;
import com.mealup.vendors.VendorRepository;
import com.mealup.common.exception.ConflictException;
import com.mealup.common.exception.BadRequestException;
import com.mealup.volunteers.VolunteerDtos.TemporaryPassword;
import com.mealup.volunteers.VolunteerDtos.VolunteerRow;

/** What the admin does with volunteer accounts: see everyone with their totals, switch accounts off and on, reset a password. */
@Service
public class VolunteerAdminService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final VolunteerStats stats;
    private final PickupService pickups;
    private final PasswordEncoder encoder;
    private final AccessPolicy access;
    private final VendorRepository vendors;
    private final PickupRepository pickupRows;
    private final com.mealup.auth.UserCheckCache userChecks;

    public VolunteerAdminService(UserRepository users, VolunteerStats stats, PickupService pickups,
                                 PasswordEncoder encoder, AccessPolicy access,
                                 VendorRepository vendors, PickupRepository pickupRows,
                                 com.mealup.auth.UserCheckCache userChecks) {
        this.userChecks = userChecks;
        this.vendors = vendors;
        this.pickupRows = pickupRows;
        this.users = users;
        this.stats = stats;
        this.pickups = pickups;
        this.encoder = encoder;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public PageResponse<VolunteerRow> list(String q, Boolean active, String sort, int page, int size) {
        access.requireUser();
        long total = stats.count(q, active);
        List<VolunteerRow> rows = stats.list(q, active, sort, page, size);
        return new PageResponse<>(rows, page, size, total, (int) Math.ceil(total / (double) size));
    }

    /** Everything (up to 5000 volunteers) for the CSV download. */
    @Transactional(readOnly = true)
    public List<VolunteerRow> all() {
        access.requireUser();
        return stats.list(null, null, "name", 0, 5000);
    }

    @Transactional(readOnly = true)
    public Page<PickupResponse> pickupsOf(Long userId, Pageable pageable) {
        access.requireUser();
        findVolunteer(userId);
        return pickups.listForVolunteer(userId, pageable);
    }

    @Transactional
    public void setActive(Long userId, boolean active) {
        access.requireUser();
        User u = findVolunteer(userId);
        u.setActive(active);
        users.save(u);
        userChecks.evict(userId);
    }

    /** Gives a volunteer hotel-staff access for one hotel. They keep the same email and password. */
    @Transactional
    public void makeHotel(Long userId, Long vendorId) {
        access.requireUser();
        User u = findVolunteer(userId);
        var hotel = vendors.findById(vendorId).orElseThrow(() -> new BadRequestException("Choose a hotel from the list."));
        if (pickupRows.countByVolunteer_IdAndStatusIn(userId, List.of(PickupStatus.PLEDGED, PickupStatus.CONFIRMED)) > 0) {
            throw new ConflictException("This person still has a pickup in progress. Let them finish or cancel it first.");
        }
        u.makeHotelStaff(hotel);
        users.save(u);
        userChecks.evict(userId);
    }

    /** Takes hotel-staff access away; the person is an ordinary volunteer again. */
    @Transactional
    public void makeVolunteer(Long userId) {
        access.requireUser();
        User u = findVolunteer(userId);
        u.makeVolunteer();
        users.save(u);
        userChecks.evict(userId);
    }

    /** Sets a new random password and returns it once. The admin gives it to the volunteer, who can change it after signing in. */
    @Transactional
    public TemporaryPassword resetPassword(Long userId) {
        access.requireUser();
        User u = findVolunteer(userId);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        u.setPasswordHash(encoder.encode(sb.toString()));
        users.save(u);
        return new TemporaryPassword(sb.toString());
    }

    private User findVolunteer(Long id) {
        return users.findById(id).filter(u -> u.getRole() == Role.VOLUNTEER || u.getRole() == Role.HOTEL)
                .orElseThrow(() -> new NotFoundException("Volunteer not found."));
    }
}
