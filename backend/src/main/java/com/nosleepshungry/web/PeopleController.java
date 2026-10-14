package com.nosleepshungry.web;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.nosleepshungry.activity.ActivityService;
import com.nosleepshungry.model.Locality;
import com.nosleepshungry.model.User;
import com.nosleepshungry.model.VolunteerApplication;
import com.nosleepshungry.repo.LocalityRepository;
import com.nosleepshungry.repo.UserRepository;
import com.nosleepshungry.repo.VolunteerApplicationRepository;
import com.nosleepshungry.security.AccessService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Localities and coordinators. Creating/changing these is Super Admin only (see SecurityConfig).
 * Only the Super Admin can grant or revoke admin access.
 */
@RestController
@RequestMapping("/api/admin")
public class PeopleController {

    public record LocalityRequest(@NotBlank @Size(max = 120) String name,
                                  @NotBlank @Size(max = 120) String city) { }

    public record CoordinatorRequest(@NotBlank @Size(max = 120) String name,
                                     @NotBlank @Email @Size(max = 150) String email,
                                     @Size(max = 20) String phone,
                                     @NotBlank @Size(min = 8, max = 72) String password,
                                     Set<Long> localityIds) { }

    /** Every field is optional: send only what you want to change. */
    public record CoordinatorUpdate(User.Status status, Set<Long> localityIds,
                                    @Size(min = 8, max = 72) String newPassword) { }

    public record ApproveRequest(@NotBlank @Email @Size(max = 150) String email,
                                 @NotBlank @Size(min = 8, max = 72) String password,
                                 Set<Long> localityIds) { }

    public record PasswordChange(@NotBlank String currentPassword,
                                 @NotBlank @Size(min = 8, max = 72) String newPassword) { }

    public record CoordinatorView(Long id, String name, String email, String phone,
                                  String status, Set<Long> localityIds) {
        static CoordinatorView of(User u) {
            return new CoordinatorView(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                    u.getStatus().name(), Set.copyOf(u.getLocalityIds()));
        }
    }

    private final LocalityRepository localities;
    private final UserRepository users;
    private final VolunteerApplicationRepository applications;
    private final PasswordEncoder encoder;
    private final AccessService access;
    private final ActivityService activity;

    public PeopleController(LocalityRepository localities, UserRepository users,
                            VolunteerApplicationRepository applications, PasswordEncoder encoder,
                            AccessService access, ActivityService activity) {
        this.localities = localities;
        this.users = users;
        this.applications = applications;
        this.encoder = encoder;
        this.access = access;
        this.activity = activity;
    }

    // ---- localities -----------------------------------------------------

    @GetMapping("/localities")
    public List<Locality> listLocalities() {
        return localities.findAllByOrderByNameAsc();
    }

    @PostMapping("/localities")
    @ResponseStatus(HttpStatus.CREATED)
    public Locality addLocality(@Valid @RequestBody LocalityRequest r) {
        String name = r.name().trim();
        String city = r.city().trim();
        if (localities.existsByNameIgnoreCaseAndCityIgnoreCase(name, city)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That locality already exists.");
        }
        Locality saved = localities.save(new Locality(name, city));
        activity.log("LOCALITY", saved.getId(), "CREATED", name + ", " + city);
        return saved;
    }

    // ---- coordinators ---------------------------------------------------

    @GetMapping("/coordinators")
    public List<CoordinatorView> listCoordinators() {
        return users.findByRoleOrderByNameAsc(User.Role.COORDINATOR).stream().map(CoordinatorView::of).toList();
    }

    @PostMapping("/coordinators")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CoordinatorView addCoordinator(@Valid @RequestBody CoordinatorRequest r) {
        User u = createCoordinator(r.name(), r.email(), r.phone(), r.password(), r.localityIds());
        activity.log("USER", u.getId(), "COORDINATOR_CREATED", u.getEmail());
        return CoordinatorView.of(u);
    }

    @PatchMapping("/coordinators/{id}")
    @Transactional
    public CoordinatorView updateCoordinator(@PathVariable Long id, @Valid @RequestBody CoordinatorUpdate r) {
        User u = users.findById(id)
                .filter(x -> x.getRole() == User.Role.COORDINATOR)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));
        if (r.status() != null) u.setStatus(r.status());
        if (r.localityIds() != null) u.setLocalityIds(validLocalities(r.localityIds()));
        if (r.newPassword() != null) u.setPasswordHash(encoder.encode(r.newPassword()));
        activity.log("USER", u.getId(), "COORDINATOR_UPDATED",
                (r.status() != null ? "status=" + r.status() + " " : "")
                + (r.localityIds() != null ? "localities=" + r.localityIds() + " " : "")
                + (r.newPassword() != null ? "password reset" : ""));
        return CoordinatorView.of(u);
    }

    /** Approve a volunteer application: creates their coordinator account. */
    @PostMapping("/volunteer-applications/{id}/approve")
    @Transactional
    public CoordinatorView approve(@PathVariable Long id, @Valid @RequestBody ApproveRequest r) {
        VolunteerApplication a = applications.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));
        if (a.getStatus() == VolunteerApplication.Status.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already approved.");
        }
        User u = createCoordinator(null, r.email(), null, r.password(), r.localityIds(), a);
        a.setStatus(VolunteerApplication.Status.APPROVED);
        activity.log("VOLUNTEER", a.getId(), "APPROVED", "coordinator account created for " + u.getEmail());
        return CoordinatorView.of(u);
    }

    // ---- my account -----------------------------------------------------

    @PostMapping("/me/password")
    @Transactional
    public java.util.Map<String, String> changeMyPassword(@Valid @RequestBody PasswordChange r) {
        User me = access.currentUser();
        if (!encoder.matches(r.currentPassword(), me.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is wrong.");
        }
        me.setPasswordHash(encoder.encode(r.newPassword()));
        users.save(me);
        activity.log("USER", me.getId(), "PASSWORD_CHANGED", null);
        return java.util.Map.of("message", "Password changed.");
    }

    // ---- helpers --------------------------------------------------------

    private User createCoordinator(String name, String email, String phone, String password, Set<Long> localityIds) {
        return createCoordinator(name, email, phone, password, localityIds, null);
    }

    private User createCoordinator(String name, String email, String phone, String password,
                                   Set<Long> localityIds, VolunteerApplication from) {
        String mail = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(mail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists.");
        }
        String finalName = from != null ? fromName(from) : name.trim();
        String finalPhone = from != null ? fromPhone(from) : (phone == null || phone.isBlank() ? null : phone.trim());
        User u = new User(finalName, mail, finalPhone, encoder.encode(password), User.Role.COORDINATOR);
        u.setLocalityIds(validLocalities(localityIds));
        return users.save(u);
    }

    // The application entity keeps its fields private, so read them through the JSON-free accessors below.
    private static String fromName(VolunteerApplication a) { return a.getName(); }
    private static String fromPhone(VolunteerApplication a) { return a.getPhone(); }

    private Set<Long> validLocalities(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();
        if (localities.findAllById(ids).size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown locality.");
        }
        return ids;
    }
}
