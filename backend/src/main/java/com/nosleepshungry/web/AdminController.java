package com.nosleepshungry.web;

import java.util.List;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.nosleepshungry.activity.ActivityService;
import com.nosleepshungry.model.FoodOffer;
import com.nosleepshungry.model.HungerReport;
import com.nosleepshungry.model.VolunteerApplication;
import com.nosleepshungry.repo.FoodOfferRepository;
import com.nosleepshungry.repo.HungerReportRepository;
import com.nosleepshungry.repo.LocalityRepository;
import com.nosleepshungry.repo.VolunteerApplicationRepository;
import com.nosleepshungry.security.AccessService;
import com.nosleepshungry.security.AccessService.Scope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Review submissions and move them through their statuses.
 * Coordinators only see (and change) records in their own localities.
 * The Super Admin sees everything, including records with no locality yet, and assigns localities.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public record OfferStatus(@NotNull FoodOffer.Status status) { }
    public record ReportStatus(@NotNull HungerReport.Status status) { }
    public record ApplicationStatus(@NotNull VolunteerApplication.Status status) { }
    public record LocalityAssign(Long localityId) { }

    private final FoodOfferRepository offers;
    private final HungerReportRepository reports;
    private final VolunteerApplicationRepository applications;
    private final LocalityRepository localities;
    private final AccessService access;
    private final ActivityService activity;

    public AdminController(FoodOfferRepository offers, HungerReportRepository reports,
                           VolunteerApplicationRepository applications, LocalityRepository localities,
                           AccessService access, ActivityService activity) {
        this.offers = offers;
        this.reports = reports;
        this.applications = applications;
        this.localities = localities;
        this.access = access;
        this.activity = activity;
    }

    // ---- food offers ----------------------------------------------------

    @GetMapping("/food-offers")
    public List<FoodOffer> listOffers(@RequestParam(required = false) FoodOffer.Status status) {
        List<FoodOffer> all = status == null ? offers.findAllByOrderByCreatedAtDesc()
                                             : offers.findByStatusOrderByCreatedAtDesc(status);
        return inScope(all, FoodOffer::getLocalityId);
    }

    @PatchMapping("/food-offers/{id}/status")
    @Transactional
    public FoodOffer setOfferStatus(@PathVariable Long id, @Valid @RequestBody OfferStatus body) {
        FoodOffer o = offers.findById(id).orElseThrow(AdminController::notFound);
        requireInScope(o.getLocalityId());
        o.setStatus(body.status());
        activity.log("FOOD_OFFER", id, "STATUS", body.status().name());
        return o;
    }

    @PatchMapping("/food-offers/{id}/locality")
    @Transactional
    public FoodOffer setOfferLocality(@PathVariable Long id, @RequestBody LocalityAssign body) {
        access.requireSuperAdmin();
        FoodOffer o = offers.findById(id).orElseThrow(AdminController::notFound);
        o.setLocalityId(validLocality(body.localityId()));
        activity.log("FOOD_OFFER", id, "LOCALITY", String.valueOf(body.localityId()));
        return o;
    }

    // ---- hunger reports -------------------------------------------------

    @GetMapping("/hunger-reports")
    public List<HungerReport> listReports(@RequestParam(required = false) HungerReport.Status status) {
        List<HungerReport> all = status == null ? reports.findAllByOrderByCreatedAtDesc()
                                                : reports.findByStatusOrderByCreatedAtDesc(status);
        return inScope(all, HungerReport::getLocalityId);
    }

    @PatchMapping("/hunger-reports/{id}/status")
    @Transactional
    public HungerReport setReportStatus(@PathVariable Long id, @Valid @RequestBody ReportStatus body) {
        HungerReport r = reports.findById(id).orElseThrow(AdminController::notFound);
        requireInScope(r.getLocalityId());
        r.setStatus(body.status());
        activity.log("HUNGER_REPORT", id, "STATUS", body.status().name());
        return r;
    }

    @PatchMapping("/hunger-reports/{id}/locality")
    @Transactional
    public HungerReport setReportLocality(@PathVariable Long id, @RequestBody LocalityAssign body) {
        access.requireSuperAdmin();
        HungerReport r = reports.findById(id).orElseThrow(AdminController::notFound);
        r.setLocalityId(validLocality(body.localityId()));
        activity.log("HUNGER_REPORT", id, "LOCALITY", String.valueOf(body.localityId()));
        return r;
    }

    // ---- volunteer applications (Super Admin only, see SecurityConfig) ---

    @GetMapping("/volunteer-applications")
    public List<VolunteerApplication> listApplications(
            @RequestParam(required = false) VolunteerApplication.Status status) {
        return status == null ? applications.findAllByOrderByCreatedAtDesc()
                              : applications.findByStatusOrderByCreatedAtDesc(status);
    }

    @PatchMapping("/volunteer-applications/{id}/status")
    @Transactional
    public VolunteerApplication setApplicationStatus(@PathVariable Long id,
                                                     @Valid @RequestBody ApplicationStatus body) {
        VolunteerApplication a = applications.findById(id).orElseThrow(AdminController::notFound);
        a.setStatus(body.status());
        activity.log("VOLUNTEER", id, "STATUS", body.status().name());
        return a;
    }

    // ---- helpers --------------------------------------------------------

    private <T> List<T> inScope(List<T> all, Function<T, Long> localityOf) {
        Scope scope = access.scope();
        return all.stream().filter(r -> scope.allows(localityOf.apply(r))).toList();
    }

    private void requireInScope(Long localityId) {
        if (!access.scope().allows(localityId)) throw notFound();   // do not reveal other localities' records
    }

    private Long validLocality(Long id) {
        if (id == null) return null;
        if (!localities.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown locality.");
        }
        return id;
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found.");
    }
}
