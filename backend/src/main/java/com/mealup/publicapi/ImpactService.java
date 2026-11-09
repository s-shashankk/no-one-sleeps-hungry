package com.mealup.publicapi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.areas.Area;
import com.mealup.areas.AreaRepository;
import com.mealup.common.exception.NotFoundException;
import com.mealup.pickups.PickupRepository;
import com.mealup.pickups.PickupStatus;
import com.mealup.publicapi.PublicDtos.AreaImpact;
import com.mealup.publicapi.PublicDtos.Impact;
import com.mealup.publicapi.PublicDtos.PublicArea;
import com.mealup.publicapi.PublicDtos.PublicHotel;
import com.mealup.publicapi.PublicDtos.PublicPartner;
import com.mealup.requests.FoodRequestRepository;
import com.mealup.vendors.LogoStore;
import com.mealup.vendors.Vendor;
import com.mealup.vendors.VendorRepository;

/** Counts and names that anyone may see. Never any phone numbers, volunteers or reporters. */
@Service
public class ImpactService {

    private final AreaRepository areas;
    private final VendorRepository vendors;
    private final PickupRepository pickups;
    private final FoodRequestRepository requests;
    private final LogoStore logos;

    public ImpactService(AreaRepository areas, VendorRepository vendors, PickupRepository pickups,
                         FoodRequestRepository requests, LogoStore logos) {
        this.logos = logos;
        this.areas = areas;
        this.vendors = vendors;
        this.pickups = pickups;
        this.requests = requests;
    }

    /** Meals served = plates from completed hotel pickups + meals delivered for completed food requests. */
    @Transactional(readOnly = true)
    public long totalMealsServed() {
        Long plates = pickups.totalPlatesServed();
        Long delivered = requests.totalMealsDelivered();
        return (plates == null ? 0 : plates) + (delivered == null ? 0 : delivered);
    }

    @Transactional(readOnly = true)
    public List<PublicArea> areaList() {
        Map<Long, Long> hotels = hotelCounts();
        return areas.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(a -> new PublicArea(a.getId(), a.getName(), a.getCity(), a.isOpen(), hotels.getOrDefault(a.getId(), 0L)))
                .toList();
    }

    /** Hotels of an open area. For an area that is "coming soon" the list is empty. */
    @Transactional(readOnly = true)
    public List<PublicHotel> hotels(Long areaId) {
        Area a = areas.findById(areaId).orElseThrow(() -> new NotFoundException("Area not found."));
        if (!a.isOpen()) return List.of();
        Map<Long, Long> booked = new HashMap<>();
        for (Object[] row : pickups.bookingsSince(areaId, com.mealup.pickups.BookingDay.startUtc())) {
            booked.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return vendors.findByArea_IdAndActiveTrueOrderByNameAsc(areaId).stream()
                .map(v -> new PublicHotel(v.getId(), v.getName(), v.getLocation(),
                        (int) Math.max(0, v.getDailyLimit() - booked.getOrDefault(v.getId(), 0L)), v.getMapUrl()))
                .toList();
    }

    /** Active hotels for the home-page slider (name, area and logo version only). */
    @Transactional(readOnly = true)
    public List<PublicPartner> partners() {
        List<Vendor> list = vendors.findByActiveTrueOrderByNameAsc();
        Map<Long, Long> versions = logos.versions(list.stream().map(Vendor::getId).toList());
        return list.stream().limit(60)
                .map(v -> new PublicPartner(v.getId(), v.getName(),
                        v.getArea() == null ? null : v.getArea().getName(), versions.get(v.getId())))
                .toList();
    }

    /** The logo of an active hotel. */
    @Transactional(readOnly = true)
    public LogoStore.Logo logo(Long vendorId) {
        Vendor v = vendors.findById(vendorId).orElse(null);
        if (v == null || !v.isActive()) throw new NotFoundException("Logo not found.");
        return logos.find(vendorId).orElseThrow(() -> new NotFoundException("Logo not found."));
    }

    @Transactional(readOnly = true)
    public Impact impact() {
        Map<Long, Long> hotels = hotelCounts();
        Map<Long, Long> plates = new HashMap<>();
        for (Object[] row : pickups.platesServedByArea()) {
            plates.put((Long) row[0], ((Number) row[1]).longValue());
        }
        List<AreaImpact> perArea = areas.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(a -> new AreaImpact(a.getId(), a.getName(), a.getCity(), a.isOpen(),
                        hotels.getOrDefault(a.getId(), 0L), plates.getOrDefault(a.getId(), 0L)))
                .toList();
        return new Impact(totalMealsServed(), areas.countOpenCities(), areas.countByOpenTrue(),
                vendors.countByActiveTrue(), pickups.countByStatus(PickupStatus.COMPLETED), perArea);
    }

    private Map<Long, Long> hotelCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : vendors.activeCountsByArea()) {
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }
}
