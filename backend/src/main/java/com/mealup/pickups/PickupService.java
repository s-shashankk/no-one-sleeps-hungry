package com.mealup.pickups;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.access.AccessPolicy;
import com.mealup.access.AuthenticatedUser;
import com.mealup.common.exception.BadRequestException;
import com.mealup.common.exception.ConflictException;
import com.mealup.common.exception.NotFoundException;
import com.mealup.common.time.Utc;
import com.mealup.common.validation.Text;
import com.mealup.pickups.PickupDtos.*;
import com.mealup.users.User;
import com.mealup.users.UserRepository;
import com.mealup.vendors.Vendor;
import com.mealup.vendors.VendorRepository;

/** Volunteer pickups: pledged by a signed-in volunteer (with the number of people), followed through by the admin until plates served are recorded. */
@Service
public class PickupService {

    /** A volunteer can have this many pickups in progress at once. */
    static final int MAX_OPEN_PER_VOLUNTEER = 3;
    private static final List<PickupStatus> OPEN = List.of(PickupStatus.PLEDGED, PickupStatus.CONFIRMED);

    private final PickupRepository pickups;
    private final VendorRepository vendors;
    private final UserRepository users;
    private final AccessPolicy access;

    public PickupService(PickupRepository pickups, VendorRepository vendors, UserRepository users, AccessPolicy access) {
        this.pickups = pickups;
        this.vendors = vendors;
        this.users = users;
        this.access = access;
    }

    // ---- signed-in volunteer -----------------------------------------------------------------

    /** The volunteer says which hotel they will collect from and for how many people. Name and phone come from their account. */
    @Transactional
    public VolunteerPickupResponse pledge(PledgeRequest req) {
        AuthenticatedUser me = access.requireVolunteer();
        User user = users.findById(me.id()).orElseThrow(() -> new NotFoundException("Account not found."));
        Vendor v = vendors.findByIdForUpdate(req.vendorId()).orElse(null);
        if (v == null || !v.isActive() || v.getArea() == null || !v.getArea().isOpen()) {
            throw new BadRequestException("This hotel is not open for pickups right now. Please choose another.");
        }
        if (pickups.countByVolunteer_IdAndStatusIn(user.getId(), OPEN) >= MAX_OPEN_PER_VOLUNTEER) {
            throw new ConflictException("You already have " + MAX_OPEN_PER_VOLUNTEER
                    + " pickups in progress. Please finish or cancel one first.");
        }
        // the hotel's daily limit counts plates: every person a volunteer will feed is one plate
        long left = v.getDailyLimit() - pickups.platesBookedSince(v.getId(), BookingDay.startUtc());
        if (left <= 0) {
            throw new ConflictException(v.getName() + " has already given all its plates for today ("
                    + v.getDailyLimit() + "). Please choose another hotel, or come back tomorrow.");
        }
        if (req.peopleCount() > left) {
            throw new ConflictException(v.getName() + " has only " + left + (left == 1 ? " plate" : " plates")
                    + " left today. Please enter " + left + " or fewer, or choose another hotel.");
        }
        Pickup p = new Pickup(v, v.getArea(), user, user.getFullName(), user.getPhoneNumber(),
                Text.clean(req.deliverTo()), req.peopleCount());
        return toVolunteerResponse(pickups.save(p));
    }

    @Transactional(readOnly = true)
    public Page<VolunteerPickupResponse> myPickups(Pageable pageable) {
        AuthenticatedUser me = access.requireVolunteer();
        return pickups.findByVolunteer_Id(me.id(), pageable).map(PickupService::toVolunteerResponse);
    }

    /** A volunteer can cancel their own pickup while it is still in progress. */
    @Transactional
    public VolunteerPickupResponse cancelMine(Long id) {
        AuthenticatedUser me = access.requireVolunteer();
        Pickup p = find(id);
        if (p.getVolunteer() == null || !p.getVolunteer().getId().equals(me.id())) {
            throw new NotFoundException("Pickup not found.");
        }
        p.cancel("Cancelled by the volunteer.");
        return toVolunteerResponse(pickups.saveAndFlush(p));
    }

    // ---- admin -------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<PickupResponse> list(PickupStatus status, Pageable pageable) {
        access.requireUser();
        Page<Pickup> page = status == null ? pickups.findAll(pageable) : pickups.findByStatus(status, pageable);
        return page.map(PickupService::toResponse);
    }

    /** All pickups of one volunteer account (admin view). */
    @Transactional(readOnly = true)
    public Page<PickupResponse> listForVolunteer(Long userId, Pageable pageable) {
        access.requireUser();
        return pickups.findByVolunteer_Id(userId, pageable).map(PickupService::toResponse);
    }

    /** The admin has told the hotel. */
    @Transactional
    public PickupResponse confirm(Long id) {
        access.requireUser();
        Pickup p = find(id);
        p.confirm();
        return toResponse(pickups.saveAndFlush(p));
    }

    /** Food collected and served: record the plates. */
    @Transactional
    public PickupResponse complete(Long id, CompletePickupRequest req) {
        access.requireUser();
        Pickup p = find(id);
        p.complete(req.platesServed(), Text.clean(req.note()));
        return toResponse(pickups.saveAndFlush(p));
    }

    /** The admin accepts the handover on the hotel's behalf: counts as served, one plate per person they said they would feed. */
    @Transactional
    public PickupResponse accept(Long id) {
        access.requireUser();
        Pickup p = find(id);
        p.complete(p.getPeopleCount(), null, "ADMIN");
        return toResponse(pickups.saveAndFlush(p));
    }

    @Transactional
    public PickupResponse cancel(Long id, PickupNoteRequest req) {
        access.requireUser();
        Pickup p = find(id);
        p.cancel(Text.clean(req.note()));
        return toResponse(pickups.saveAndFlush(p));
    }

    @Transactional
    public PickupResponse reopen(Long id) {
        access.requireUser();
        Pickup p = find(id);
        p.reopen();
        return toResponse(pickups.saveAndFlush(p));
    }

    private Pickup find(Long id) {
        return pickups.findById(id).orElseThrow(() -> new NotFoundException("Pickup not found."));
    }

    private static PickupResponse toResponse(Pickup p) {
        Vendor v = p.getVendor();
        return new PickupResponse(p.getId(), p.getStatus(),
                v.getId(), v.getName(), v.getPhoneNumber(), v.getLocation(),
                p.getArea().getId(), p.getArea().getName(), p.getArea().getCity(),
                p.getVolunteer() == null ? null : p.getVolunteer().getId(),
                p.getVolunteer() == null ? null : p.getVolunteer().getEmail(),
                p.getVolunteerName(), p.getVolunteerPhone(), p.getDeliverTo(),
                p.getPeopleCount(), p.getPlatesServed(), p.getNote(),
                Utc.toInstant(p.getCreatedAt()), Utc.toInstant(p.getClosedAt()),
                p.getClosedBy());
    }

    private static VolunteerPickupResponse toVolunteerResponse(Pickup p) {
        return new VolunteerPickupResponse(p.getId(), p.getStatus(), p.getVendor().getName(), p.getVendor().getLocation(),
                p.getArea().getName(), p.getPeopleCount(), p.getDeliverTo(), p.getPlatesServed(),
                Utc.toInstant(p.getCreatedAt()), Utc.toInstant(p.getClosedAt()), p.getClosedBy(), p.getVendor().getMapUrl());
    }
}
