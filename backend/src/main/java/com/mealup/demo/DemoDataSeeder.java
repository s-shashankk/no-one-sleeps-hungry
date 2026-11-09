package com.mealup.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mealup.areas.Area;
import com.mealup.areas.AreaRepository;
import com.mealup.coworkers.Coworker;
import com.mealup.coworkers.CoworkerRepository;
import com.mealup.feedback.Feedback;
import com.mealup.feedback.FeedbackKind;
import com.mealup.feedback.FeedbackRepository;
import com.mealup.joins.JoinRequest;
import com.mealup.joins.JoinRequestRepository;
import com.mealup.pickups.Pickup;
import com.mealup.pickups.PickupRepository;
import com.mealup.users.User;
import com.mealup.users.UserRepository;
import com.mealup.vendors.LogoStore;
import com.mealup.vendors.Vendor;
import com.mealup.vendors.VendorRepository;

/**
 * Sample hotels, volunteers, one pickup and one join request, so the whole flow can be tried straight away.
 * Only runs when mealup.demo-data=true (on by default in the dev profile; in production set MEALUP_DEMO_DATA=true
 * for a demo, never for the live site). Everything is marked "(sample)": edit it or switch it off in the admin.
 * Safe to run again: it never creates the same hotel or volunteer twice.
 */
@Component
@Order(2)                       // after the admin account has been created
@ConditionalOnProperty(name = "mealup.demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String NOTE = "Sample data. Edit it, or switch it off.";

    private final AreaRepository areas;
    private final VendorRepository vendors;
    private final CoworkerRepository coworkers;
    private final PickupRepository pickups;
    private final JoinRequestRepository joins;
    private final LogoStore logos;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final FeedbackRepository feedback;

    public DemoDataSeeder(AreaRepository areas, VendorRepository vendors, CoworkerRepository coworkers,
                          PickupRepository pickups, JoinRequestRepository joins, LogoStore logos,
                          UserRepository users, PasswordEncoder encoder, FeedbackRepository feedback) {
        this.feedback = feedback;
        this.logos = logos;
        this.users = users;
        this.encoder = encoder;
        this.areas = areas;
        this.vendors = vendors;
        this.coworkers = coworkers;
        this.pickups = pickups;
        this.joins = joins;
    }

    /** logo = file name in resources/demo-logos (PNG drawn for the demo), or null for a hotel without a logo yet. */
    private record Hotel(String name, String contact, String phone, String address, String area, String logo) { }

    /** Sample volunteer accounts (password Sample#Pass123) with a few pickups each, so the admin Volunteers page has data. */
    private void seedVolunteerAccounts(List<Area> allAreas) {
        String password = "Sample#Pass123";
        String[][] people = {
                {"Ravi Kumar (sample)", "ravi.sample@mealup.local", "+919000000051"},
                {"Ananya S (sample)", "ananya.sample@mealup.local", "+919000000052"},
                {"Imran Khan (sample)", "imran.sample@mealup.local", "+919000000053"}};
        Area indiranagar = allAreas.stream().filter(a -> a.getName().equalsIgnoreCase("Indiranagar")).findFirst().orElse(null);
        Vendor hotelA = vendors.findFirstByNameIgnoreCase("Sunrise Residency (sample)").orElse(null);
        Vendor hotelB = vendors.findFirstByNameIgnoreCase("Green Leaf Meals (sample)").orElse(null);
        Vendor hotelC = vendors.findFirstByNameIgnoreCase("Banyan Tree Biryani (sample)").orElse(null);
        for (String[] p : people) {
            if (users.existsByEmailIgnoreCase(p[1]) || users.existsByPhoneNumber(p[2])) continue;
            User u = users.save(User.volunteer(p[0], p[2], p[1], encoder.encode(password)));
            if (indiranagar == null || hotelA == null || hotelB == null || hotelC == null) continue;
            if (p[1].startsWith("ravi")) {
                done(hotelA, indiranagar, u, 40, 48);
                done(hotelB, indiranagar, u, 30, 35);
            } else if (p[1].startsWith("ananya")) {
                done(hotelC, indiranagar, u, 55, 60);
                pickups.save(new Pickup(hotelA, indiranagar, u, u.getFullName(), u.getPhoneNumber(), "Shelter near the market", 20));
            }
        }
        if (hotelA != null && !users.existsByEmailIgnoreCase("hotel.sample@mealup.local")) {
            User staff = User.volunteer("Mr. Shetty (sample)", "+919000000061", "hotel.sample@mealup.local", encoder.encode(password));
            staff.makeHotelStaff(hotelA);
            users.save(staff);
        }
        log.info("Sample hotel-staff login (Sunrise Residency): hotel.sample@mealup.local, same password");
        log.info("Sample volunteer logins: ravi.sample@mealup.local / ananya.sample@mealup.local / imran.sample@mealup.local, password {}", password);
    }

    /**
     * A few bookings in every state and some feedback, so the hotel page, the admin Pickups page and the Messages page
     * can be tried at once. Runs only while there is no feedback yet (so it never piles up). All of it can be edited or removed.
     */
    private void seedWorkflowSamples(List<Area> allAreas) {
        if (feedback.count() > 0) return;
        Area indiranagar = allAreas.stream().filter(a -> a.getName().equalsIgnoreCase("Indiranagar")).findFirst().orElse(null);
        Vendor sunrise = vendors.findFirstByNameIgnoreCase("Sunrise Residency (sample)").orElse(null);
        User ravi = users.findByEmailIgnoreCase("ravi.sample@mealup.local").orElse(null);
        User ananya = users.findByEmailIgnoreCase("ananya.sample@mealup.local").orElse(null);
        User imran = users.findByEmailIgnoreCase("imran.sample@mealup.local").orElse(null);
        User staff = users.findByEmailIgnoreCase("hotel.sample@mealup.local").orElse(null);
        if (sunrise == null || indiranagar == null || ravi == null || ananya == null || imran == null) return;

        sunrise.setDailyLimit(8);
        vendors.save(sunrise);

        // waiting for the hotel to accept (the hotel staff sample login sees these two)
        pickups.save(new Pickup(sunrise, indiranagar, imran, imran.getFullName(), imran.getPhoneNumber(), "Old-age home near the lake", 18));
        pickups.save(new Pickup(sunrise, indiranagar, ravi, ravi.getFullName(), ravi.getPhoneNumber(), "Footpath near the bus stand", 25));
        // already accepted by the hotel, and one accepted by the admin on the hotel's behalf
        Pickup byHotel = new Pickup(sunrise, indiranagar, ananya, ananya.getFullName(), ananya.getPhoneNumber(), "Shelter near the market", 30);
        byHotel.complete(30, null, "HOTEL");
        pickups.save(byHotel);
        Pickup byAdmin = new Pickup(sunrise, indiranagar, imran, imran.getFullName(), imran.getPhoneNumber(), "Children's home", 22);
        byAdmin.complete(22, null, "ADMIN");
        pickups.save(byAdmin);

        if (staff != null) {
            feedback.save(new Feedback(staff, FeedbackKind.FEEDBACK, 5, "The volunteers were polite and came on time. Our kitchen team felt happy to see the food going to people who needed it."));
            feedback.save(new Feedback(staff, FeedbackKind.MESSAGE, null, "We have a wedding event on Saturday evening, so we will be very busy and may have a lot of extra food after 10 pm. Please send volunteers late."));
        }
        feedback.save(new Feedback(ravi, FeedbackKind.MESSAGE, null, "There is an old woman near the bus stand who cannot walk to the shelter. Can we take food to her every day?"));
        feedback.save(new Feedback(ananya, FeedbackKind.FEEDBACK, 4, "Booking was very easy. It would help if the page showed the hotel's opening hours."));
        feedback.save(new Feedback(imran, FeedbackKind.FEEDBACK, 5, "A child hugged me today when I gave the food. Thank you for building this."));
        log.info("Sample bookings and feedback are ready: sign in as hotel.sample@mealup.local to accept the two waiting bookings.");
    }

    private void done(Vendor hotel, Area area, User u, int people, int plates) {
        Pickup p = new Pickup(hotel, area, u, u.getFullName(), u.getPhoneNumber(), "Footpath near the market", people);
        p.complete(plates, "Sample pickup");
        pickups.save(p);
    }

    private static byte[] readLogo(String name) {
        try (InputStream in = new ClassPathResource("demo-logos/" + name + ".png").getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            log.warn("Sample logo {} could not be read: {}", name, e.getMessage());
            return null;
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Hotel> hotels = List.of(
                new Hotel("Sunrise Residency (sample)", "Mr. Shetty", "+919000000001", "100 Feet Road, Indiranagar", "Indiranagar", "sunrise-residency"),
                new Hotel("Green Leaf Meals (sample)", "Mrs. Rao", "+919000000002", "12th Main, Indiranagar", "Indiranagar", "green-leaf-meals"),
                new Hotel("Royal Spice Banquet (sample)", "Mr. Khan", "+919000000003", "CMH Road, Indiranagar", "Indiranagar", null),
                new Hotel("Lakeview Tiffins (sample)", "Mr. Gowda", "+919000000004", "Vijayanagar Main Road", "Vijayanagar", null),
                new Hotel("Silver Oak Caterers (sample)", "Ms. D'Souza", "+919000000005", "Old Airport Road, Domlur", "Domlur", null),
                new Hotel("Banyan Tree Biryani (sample)", "Mr. Basha", "+919000000041", "80 Feet Road, Indiranagar", "Indiranagar", "banyan-tree-biryani"),
                new Hotel("Coastal Curry House (sample)", "Mrs. Pinto", "+919000000042", "5th Cross, Indiranagar", "Indiranagar", "coastal-curry-house"),
                new Hotel("Tandoor Terrace (sample)", "Mr. Singh", "+919000000043", "Double Road, Indiranagar", "Indiranagar", "tandoor-terrace"),
                new Hotel("Amba Bhavan Tiffins (sample)", "Mr. Murthy", "+919000000044", "Vijayanagar 2nd Stage", "Vijayanagar", "amba-bhavan-tiffins"),
                new Hotel("Saffron Court Hotel (sample)", "Ms. Iyer", "+919000000045", "Domlur Layout", "Domlur", "saffron-court-hotel"),
                new Hotel("Jasmine Garden Restaurant (sample)", "Mrs. Fernandes", "+919000000046", "Vijayanagar Market Road", "Vijayanagar", "jasmine-garden"));

        List<Area> allAreas = areas.findAllByOrderBySortOrderAscNameAsc();
        Vendor first = null;
        for (Hotel h : hotels) {
            Vendor v = vendors.findFirstByNameIgnoreCase(h.name()).orElse(null);
            if (v == null) {
                Area area = allAreas.stream().filter(a -> a.getName().equalsIgnoreCase(h.area())).findFirst().orElse(null);
                v = new Vendor(h.name(), h.contact(), h.phone(), h.address(), NOTE);
                v.setArea(area);
                v = vendors.save(v);
                if (first == null) first = v;
            }
            // A sample logo, only when the hotel has none (so a logo you upload yourself is never replaced).
            if (h.logo() != null && logos.version(v.getId()) == null) {
                byte[] png = readLogo(h.logo());
                if (png != null) logos.save(v.getId(), "image/png", png);
            }
        }

        String[][] volunteers = {
                {"Ravi Kumar (sample)", "+919000000011", "Indiranagar"},
                {"Ananya S (sample)", "+919000000012", "Indiranagar"},
                {"Imran Khan (sample)", "+919000000013", "Domlur"},
                {"Lakshmi N (sample)", "+919000000014", "Vijayanagar"}};
        for (String[] vol : volunteers) {
            if (!coworkers.existsByPhoneNumber(vol[1])) {
                coworkers.save(new Coworker(vol[0], vol[1], vol[2], NOTE));
            }
        }

        seedVolunteerAccounts(allAreas);
        seedWorkflowSamples(allAreas);

        // One waiting pickup and one finished pickup, only when there are none yet.
        if (first != null && pickups.count() == 0) {
            Area indiranagar = areas.findAllByOrderBySortOrderAscNameAsc().stream()
                    .filter(a -> a.getName().equalsIgnoreCase("Indiranagar")).findFirst().orElse(null);
            if (indiranagar != null) {
                pickups.save(new Pickup(first, indiranagar, null, "Meera Nair (sample)", "+919000000021", "Shelter near the flyover", 25));
                Pickup done = new Pickup(first, indiranagar, null, "Ravi Kumar (sample)", "+919000000011", "Footpath near the market", 40);
                done.complete(48, "Sample pickup");
                pickups.save(done);
            }
        }
        if (joins.count() == 0) {
            joins.save(new JoinRequest("Kiran (sample)", "+919000000031", null, "Domlur", "I have a bike and free weekends."));
        }
        log.info("Sample data is ready (mealup.demo-data=true). Edit or switch it off in the admin pages.");
    }
}
