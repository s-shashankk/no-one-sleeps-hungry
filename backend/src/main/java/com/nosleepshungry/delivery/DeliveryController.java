package com.nosleepshungry.delivery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.nosleepshungry.activity.ActivityService;
import com.nosleepshungry.model.FoodOffer;
import com.nosleepshungry.model.HungerReport;
import com.nosleepshungry.repo.FoodOfferRepository;
import com.nosleepshungry.repo.HungerReportRepository;
import com.nosleepshungry.security.AccessService;
import com.nosleepshungry.security.AccessService.Scope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Matches one food offer to the hunger reports it will serve and tracks the delivery.
 * Keeps the statuses of the offer and the reports in step with the delivery.
 */
@RestController
@RequestMapping("/api/admin/deliveries")
public class DeliveryController {

    public record CreateRequest(
            @NotNull Long foodOfferId,
            @NotEmpty(message = "choose at least one hunger report") Set<Long> hungerReportIds,
            @NotBlank @Size(max = 120) String volunteerName,
            @AssertTrue(message = "please confirm the food was checked") boolean safetyChecked,
            @Size(max = 1000) String notes) { }

    public record StatusRequest(@NotNull Delivery.Status status,
                                @Min(0) Integer peopleServed,
                                @Size(max = 1000) String notes) { }

    public record OfferSummary(Long id, String description, int servings, String pickupAddress,
                               String area, LocalDateTime expiresAt) { }

    public record ReportSummary(Long id, String locationText, String area, int peopleCount,
                                Double latitude, Double longitude) { }

    public record DeliveryView(Long id, String status, Long localityId, String volunteerName, String notes,
                               Integer peopleServed, LocalDateTime createdAt, LocalDateTime pickedUpAt,
                               LocalDateTime deliveredAt, String createdBy,
                               OfferSummary offer, List<ReportSummary> reports) { }

    private final DeliveryRepository deliveries;
    private final FoodOfferRepository offers;
    private final HungerReportRepository reports;
    private final AccessService access;
    private final ActivityService activity;

    public DeliveryController(DeliveryRepository deliveries, FoodOfferRepository offers,
                              HungerReportRepository reports, AccessService access, ActivityService activity) {
        this.deliveries = deliveries;
        this.offers = offers;
        this.reports = reports;
        this.access = access;
        this.activity = activity;
    }

    @GetMapping
    public List<DeliveryView> list(@RequestParam(required = false) Delivery.Status status) {
        Scope scope = access.scope();
        List<Delivery> all = status == null ? deliveries.findAllByOrderByCreatedAtDesc()
                                            : deliveries.findByStatusOrderByCreatedAtDesc(status);
        return all.stream().filter(d -> scope.allows(d.getLocalityId())).map(this::view).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public DeliveryView create(@Valid @RequestBody CreateRequest r) {
        Scope scope = access.scope();
        FoodOffer offer = offers.findById(r.foodOfferId())
                .filter(o -> scope.allows(o.getLocalityId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food offer not found."));
        if (offer.getLocalityId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assign a locality to the food offer first.");
        }
        if (offer.getStatus() != FoodOffer.Status.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This food offer is no longer open.");
        }
        if (offer.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This food has passed its safe-until time.");
        }

        List<HungerReport> picked = reports.findAllById(r.hungerReportIds());
        if (picked.size() != r.hungerReportIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown hunger report.");
        }
        for (HungerReport h : picked) {
            if (!offer.getLocalityId().equals(h.getLocalityId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "All hunger reports must be in the same locality as the food offer.");
            }
            HungerReport.Status s = h.getStatus();
            if (s != HungerReport.Status.NEW && s != HungerReport.Status.VERIFIED && s != HungerReport.Status.RECURRING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Hunger report #" + h.getId() + " is already scheduled or closed.");
            }
        }

        String notes = r.notes() == null || r.notes().isBlank() ? null : r.notes().trim();
        Delivery d = deliveries.save(new Delivery(offer.getId(), offer.getLocalityId(),
                access.currentUser().getEmail(), r.volunteerName().trim(), notes, r.hungerReportIds()));
        offer.setStatus(FoodOffer.Status.ASSIGNED);
        picked.forEach(h -> h.setStatus(HungerReport.Status.SCHEDULED));
        activity.log("DELIVERY", d.getId(), "CREATED",
                "offer #" + offer.getId() + " -> reports " + r.hungerReportIds());
        return view(d);
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public DeliveryView setStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        Scope scope = access.scope();
        Delivery d = deliveries.findById(id)
                .filter(x -> scope.allows(x.getLocalityId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));

        Delivery.Status from = d.getStatus();
        Delivery.Status to = r.status();
        boolean allowed = (from == Delivery.Status.ASSIGNED
                    && (to == Delivery.Status.PICKED_UP || to == Delivery.Status.CANCELLED))
                || (from == Delivery.Status.PICKED_UP
                    && (to == Delivery.Status.DELIVERED || to == Delivery.Status.FAILED));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change a delivery from " + from + " to " + to + ".");
        }

        FoodOffer offer = offers.findById(d.getFoodOfferId()).orElseThrow();
        List<HungerReport> linked = reports.findAllById(d.getHungerReportIds());
        LocalDateTime now = LocalDateTime.now();

        switch (to) {
            case PICKED_UP -> {
                d.setPickedUpAt(now);
                offer.setStatus(FoodOffer.Status.IN_TRANSIT);
            }
            case DELIVERED -> {
                int total = linked.stream().mapToInt(HungerReport::getPeopleCount).sum();
                d.setDeliveredAt(now);
                d.setPeopleServed(r.peopleServed() != null ? r.peopleServed() : total);
                offer.setStatus(FoodOffer.Status.COMPLETED);
                linked.forEach(h -> h.setStatus(HungerReport.Status.SERVED));
            }
            case CANCELLED -> {
                // nothing was collected: the food goes back on the list (unless it has expired)
                offer.setStatus(offer.getExpiresAt().isBefore(now) ? FoodOffer.Status.EXPIRED : FoodOffer.Status.OPEN);
                linked.forEach(h -> h.setStatus(HungerReport.Status.VERIFIED));
            }
            case FAILED -> {
                // collected but could not be delivered (e.g. spoiled): the food is not reusable
                offer.setStatus(FoodOffer.Status.CANCELLED);
                linked.forEach(h -> h.setStatus(HungerReport.Status.VERIFIED));
            }
            default -> { }
        }
        d.setStatus(to);
        if (r.notes() != null && !r.notes().isBlank()) d.setNotes(r.notes().trim());
        activity.log("DELIVERY", d.getId(), "STATUS", from + " -> " + to
                + (d.getPeopleServed() != null && to == Delivery.Status.DELIVERED ? " (" + d.getPeopleServed() + " people)" : ""));
        return view(d);
    }

    private DeliveryView view(Delivery d) {
        OfferSummary offer = offers.findById(d.getFoodOfferId())
                .map(o -> new OfferSummary(o.getId(), o.getDescription(), o.getServings(),
                        o.getPickupAddress(), o.getArea(), o.getExpiresAt()))
                .orElse(null);
        List<ReportSummary> linked = reports.findAllById(d.getHungerReportIds()).stream()
                .map(h -> new ReportSummary(h.getId(), h.getLocationText(), h.getArea(), h.getPeopleCount(),
                        h.getLatitude(), h.getLongitude()))
                .toList();
        return new DeliveryView(d.getId(), d.getStatus().name(), d.getLocalityId(), d.getVolunteerName(),
                d.getNotes(), d.getPeopleServed(), d.getCreatedAt(), d.getPickedUpAt(), d.getDeliveredAt(),
                d.getCreatedBy(), offer, linked);
    }
}
