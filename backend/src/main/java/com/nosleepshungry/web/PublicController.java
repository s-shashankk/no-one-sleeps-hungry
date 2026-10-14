package com.nosleepshungry.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.nosleepshungry.model.FoodOffer;
import com.nosleepshungry.model.HungerReport;
import com.nosleepshungry.model.Locality;
import com.nosleepshungry.model.VolunteerApplication;
import com.nosleepshungry.repo.FoodOfferRepository;
import com.nosleepshungry.repo.HungerReportRepository;
import com.nosleepshungry.repo.LocalityRepository;
import com.nosleepshungry.repo.VolunteerApplicationRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Public endpoints: no login. Anything that stores data has a hidden
 * "website" honeypot field - real people never fill it, bots do.
 */
@RestController
@RequestMapping("/api")
public class PublicController {

    private static final String PHONE = "^[+0-9][0-9 \\-]{7,18}$";

    private final FoodOfferRepository offers;
    private final HungerReportRepository reports;
    private final VolunteerApplicationRepository applications;
    private final LocalityRepository localities;

    public PublicController(FoodOfferRepository offers, HungerReportRepository reports,
                            VolunteerApplicationRepository applications, LocalityRepository localities) {
        this.offers = offers;
        this.reports = reports;
        this.applications = applications;
        this.localities = localities;
    }

    // ---- request bodies -------------------------------------------------

    public record FoodOfferRequest(
            @Size(max = 150) String vendorName,
            @NotBlank @Size(max = 120) String submitterName,
            @NotBlank @Pattern(regexp = PHONE, message = "enter a valid phone number") String submitterPhone,
            @NotBlank @Size(max = 150) String area,
            Long localityId,
            @NotBlank @Size(max = 500) String pickupAddress,
            @NotBlank @Size(max = 1000) String description,
            @Min(1) @Max(100000) int servings,
            @NotNull FoodOffer.FoodType foodType,
            LocalDateTime cookedAt,
            @NotNull @Future(message = "must be in the future") LocalDateTime expiresAt,
            String website) { }

    public record HungerReportRequest(
            @NotBlank @Size(max = 150) String area,
            Long localityId,
            @NotBlank @Size(max = 500) String locationText,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @Min(1) @Max(100000) int peopleCount,
            @Size(max = 1000) String notes,
            @Size(max = 120) String reporterName,
            @Pattern(regexp = "^$|" + PHONE, message = "enter a valid phone number") String reporterPhone,
            String website) { }

    public record VolunteerRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = PHONE, message = "enter a valid phone number") String phone,
            @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 150) String area,
            Long localityId,
            @Size(max = 200) String collegeOrOccupation,
            @Size(max = 1000) String message,
            String website) { }

    public record LocalityView(Long id, String name, String city) { }

    // ---- endpoints ------------------------------------------------------

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/public/localities")
    public List<LocalityView> listLocalities() {
        return localities.findAllByOrderByNameAsc().stream()
                .map(l -> new LocalityView(l.getId(), l.getName(), l.getCity())).toList();
    }

    @PostMapping("/public/food-offers")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> offerFood(@Valid @RequestBody FoodOfferRequest r) {
        if (isBot(r.website())) return ok();
        FoodOffer o = new FoodOffer(blankToNull(r.vendorName()), r.submitterName().trim(),
                r.submitterPhone().trim(), r.area().trim(), r.pickupAddress().trim(), r.description().trim(),
                r.servings(), r.foodType(), r.cookedAt(), r.expiresAt());
        o.setLocalityId(resolveLocality(r.localityId()));
        FoodOffer saved = offers.save(o);
        return Map.of("id", saved.getId(), "message", "Thank you! A coordinator will contact you shortly.");
    }

    @PostMapping("/public/hunger-reports")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> reportHunger(@Valid @RequestBody HungerReportRequest r) {
        if (isBot(r.website())) return ok();
        HungerReport h = new HungerReport(r.area().trim(), r.locationText().trim(),
                r.latitude(), r.longitude(), r.peopleCount(), blankToNull(r.notes()),
                blankToNull(r.reporterName()), blankToNull(r.reporterPhone()));
        h.setLocalityId(resolveLocality(r.localityId()));
        HungerReport saved = reports.save(h);
        return Map.of("id", saved.getId(), "message", "Thank you for letting us know. We will try to help.");
    }

    @PostMapping("/public/volunteer-applications")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> volunteer(@Valid @RequestBody VolunteerRequest r) {
        if (isBot(r.website())) return ok();
        VolunteerApplication a = new VolunteerApplication(r.name().trim(), r.phone().trim(),
                blankToNull(r.email()), r.area().trim(), blankToNull(r.collegeOrOccupation()),
                blankToNull(r.message()));
        a.setLocalityId(resolveLocality(r.localityId()));
        VolunteerApplication saved = applications.save(a);
        return Map.of("id", saved.getId(), "message", "Thank you for volunteering! We will get in touch.");
    }

    /** Public numbers: counts only, never personal data. */
    @GetMapping("/public/stats")
    public Map<String, Long> stats() {
        return Map.of(
                "offersReceived", offers.count(),
                "offersCompleted", offers.countByStatus(FoodOffer.Status.COMPLETED),
                "reportsReceived", reports.count(),
                "reportsServed", reports.countByStatus(HungerReport.Status.SERVED),
                "volunteersApplied", applications.count());
    }

    // ---- helpers --------------------------------------------------------

    /**
     * Use the locality the person picked if it exists. If they picked none and there is exactly
     * one locality (we start in one area), use that. Otherwise leave it for the Super Admin to assign.
     */
    private Long resolveLocality(Long picked) {
        if (picked != null && localities.existsById(picked)) return picked;
        List<Locality> all = localities.findAll();
        return all.size() == 1 ? all.get(0).getId() : null;
    }

    private static boolean isBot(String honeypot) {
        return honeypot != null && !honeypot.isBlank();
    }

    private static Map<String, Object> ok() {
        // Pretend success so bots learn nothing.
        return Map.of("id", 0, "message", "Thank you!");
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
