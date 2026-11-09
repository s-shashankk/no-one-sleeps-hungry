package com.mealup.hotel;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.access.AccessPolicy;
import com.mealup.common.exception.ConflictException;
import com.mealup.common.exception.NotFoundException;
import com.mealup.common.time.Utc;
import com.mealup.common.validation.Text;
import com.mealup.hotel.HotelDtos.*;
import com.mealup.pickups.BookingDay;
import com.mealup.pickups.Pickup;
import com.mealup.pickups.PickupRepository;
import com.mealup.pickups.PickupStatus;
import com.mealup.users.User;
import com.mealup.users.UserRepository;
import com.mealup.vendors.Vendor;

/**
 * What a hotel's own staff can do: see who is coming to collect food, and press Accept when the food has been given.
 * They only ever see the pickups of the one hotel their account is linked to.
 */
@Service
public class HotelService {

    private static final List<PickupStatus> OPEN = List.of(PickupStatus.PLEDGED, PickupStatus.CONFIRMED);

    private final PickupRepository pickups;
    private final UserRepository users;
    private final AccessPolicy access;

    public HotelService(PickupRepository pickups, UserRepository users, AccessPolicy access) {
        this.pickups = pickups;
        this.users = users;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public HotelSummary summary() {
        Vendor v = myHotel();
        long today = pickups.platesBookedSince(v.getId(), BookingDay.startUtc());   // plates promised today
        return new HotelSummary(v.getName(), v.getLocation(), v.getDailyLimit(), today,
                pickups.countByVendor_IdAndStatusIn(v.getId(), OPEN));
    }

    /** open = true: people still to come (oldest first is handled by the caller's sort); false: everything. */
    @Transactional(readOnly = true)
    public Page<HotelPickup> list(boolean open, Pageable pageable) {
        Vendor v = myHotel();
        Page<Pickup> page = open
                ? pickups.findByVendor_IdAndStatusIn(v.getId(), OPEN, pageable)
                : pickups.findByVendor_Id(v.getId(), pageable);
        return page.map(HotelService::toDto);
    }

    /** The hotel accepts: the food is handed over and the meal counts as served (one plate per person the volunteer said they would feed). */
    @Transactional
    public HotelPickup accept(Long id) {
        Pickup p = mine(id);
        p.complete(p.getPeopleCount(), null, "HOTEL");
        return toDto(pickups.saveAndFlush(p));
    }

    /** The hotel cannot give food to this volunteer after all. */
    @Transactional
    public HotelPickup decline(Long id, DeclineRequest req) {
        Pickup p = mine(id);
        String note = Text.clean(req.note());
        p.cancel(note == null ? "The hotel could not give food this time." : "Hotel: " + note);
        return toDto(pickups.saveAndFlush(p));
    }

    private Pickup mine(Long id) {
        Vendor v = myHotel();
        Pickup p = pickups.findById(id).orElseThrow(() -> new NotFoundException("Booking not found."));
        if (!p.getVendor().getId().equals(v.getId())) throw new NotFoundException("Booking not found.");
        return p;
    }

    private Vendor myHotel() {
        var me = access.requireHotel();
        User u = users.findById(me.id()).orElseThrow(() -> new NotFoundException("Account not found."));
        if (u.getVendor() == null) throw new ConflictException("Your account is not linked to a hotel yet. Please ask the helpsomeone.in team.");
        return u.getVendor();
    }

    private static HotelPickup toDto(Pickup p) {
        return new HotelPickup(p.getId(), p.getStatus(), p.getVolunteerName(), p.getVolunteerPhone(),
                p.getPeopleCount(), p.getDeliverTo(), p.getClosedBy(),
                Utc.toInstant(p.getCreatedAt()), Utc.toInstant(p.getClosedAt()));
    }
}
