package com.mealup.demo;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.mealup.areas.Area;
import com.mealup.areas.AreaRepository;
import com.mealup.common.validation.Phones;
import com.mealup.pickups.Pickup;
import com.mealup.pickups.PickupRepository;
import com.mealup.vendors.Vendor;
import com.mealup.vendors.VendorRepository;
import com.mealup.vendors.VendorService;

/**
 * Creates the real starting data on a new server: the open area, the one partner hotel, and a single finished
 * "served before launch" record, so the website starts with the plates already given out.
 * Runs only when mealup.launch.hotel-name is set, and it never changes or duplicates what already exists.
 */
@Component
@Order(2)                       // after the admin (1), before the personal logins (3) that are linked to this hotel
@ConditionalOnProperty(name = "mealup.launch.hotel-name")
public class LaunchDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LaunchDataSeeder.class);
    static final String OPENING_NOTE = "Served before the website launched (opening balance).";

    private final LaunchProperties props;
    private final AreaRepository areas;
    private final VendorRepository vendors;
    private final PickupRepository pickups;
    private final JdbcTemplate jdbc;

    public LaunchDataSeeder(LaunchProperties props, AreaRepository areas, VendorRepository vendors,
                            PickupRepository pickups, JdbcTemplate jdbc) {
        this.props = props;
        this.areas = areas;
        this.vendors = vendors;
        this.pickups = pickups;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        String phone = props.hotelPhone() == null ? "" : Phones.normalise(props.hotelPhone());
        if (isBlank(props.hotelName()) || isBlank(props.hotelAddress()) || !Phones.isValid(phone)) {
            log.warn("Launch data skipped: hotel-name, hotel-address and a valid hotel-phone are needed in application-local.yml.");
            return;
        }
        String areaName = isBlank(props.areaName()) ? "Indiranagar" : props.areaName().trim();
        String city = isBlank(props.areaCity()) ? "Bengaluru" : props.areaCity().trim();

        Area area = areas.findAllByOrderBySortOrderAscNameAsc().stream()
                .filter(a -> a.getName().equalsIgnoreCase(areaName)).findFirst()
                .orElseGet(() -> areas.save(new Area(city, areaName, true, 1)));

        Vendor hotel = vendors.findFirstByNameIgnoreCase(props.hotelName().trim()).orElse(null);
        if (hotel == null) {
            hotel = new Vendor(props.hotelName().trim(), blankToNull(props.hotelContact()), phone,
                    props.hotelAddress().trim(), null);
            hotel.setArea(area);
            hotel.setMapUrl(VendorService.cleanMapUrl(props.hotelMapUrl()));
            hotel = vendors.save(hotel);
            log.info("Created the partner hotel {}.", hotel.getName());
        }

        int plates = props.openingPlates() == null ? 0 : props.openingPlates();
        if (plates > 0 && !pickups.existsByVendor_IdAndNote(hotel.getId(), OPENING_NOTE)) {
            Pickup p = new Pickup(hotel, area, null, "Before launch", "-", null, plates);
            p.complete(plates, OPENING_NOTE, "ADMIN");
            p = pickups.saveAndFlush(p);
            // dated two days back, so these plates do not use up the hotel's plates for today
            LocalDateTime then = LocalDateTime.now(ZoneOffset.UTC).minusDays(2);
            jdbc.update("UPDATE pickups SET created_at = ?, updated_at = ?, closed_at = ? WHERE id = ?", then, then, then, p.getId());
            log.info("Recorded {} plates served before launch at {}.", plates, hotel.getName());
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String blankToNull(String s) { return isBlank(s) ? null : s.trim(); }
}
