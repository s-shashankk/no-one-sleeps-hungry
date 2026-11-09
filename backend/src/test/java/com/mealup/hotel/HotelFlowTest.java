package com.mealup.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.mealup.support.ApiTestSupport;

/** Hotel staff: accepting a booking, the daily limit, and who may do what. */
class HotelFlowTest extends ApiTestSupport {

    private long openArea(MockHttpSession admin) throws Exception {
        return createdId("/api/v1/areas",
                Map.of("city", "Testville", "name", "Area-" + UUID.randomUUID().toString().substring(0, 8), "open", true), admin);
    }

    private long hotelIn(long area, int dailyLimit, MockHttpSession admin) throws Exception {
        Map<String, Object> hotel = vendorBody();
        hotel.put("areaId", area);
        hotel.put("dailyLimit", dailyLimit);
        return createdId("/api/v1/vendors", hotel, admin);
    }

    /** A volunteer account that the admin turns into staff of this hotel; returns a fresh signed-in session. */
    private MockHttpSession hotelStaff(long hotel, MockHttpSession admin) throws Exception {
        String email = uniqueEmail();
        registerVolunteer(email);
        long id = volunteerId(email, admin);
        postJson("/api/v1/volunteers/" + id + "/make-hotel", Map.of("vendorId", hotel), admin).andExpect(status().isOk());
        return login(email, PASSWORD);
    }

    private long volunteerId(String email, MockHttpSession admin) throws Exception {
        String q = email.substring(0, email.indexOf('@'));
        JsonNode page = json.readTree(getAs(admin, "/api/v1/volunteers?q=" + q).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        return page.get("content").get(0).get("id").asLong();
    }

    private Map<String, Object> pledge(long hotel, int people) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vendorId", hotel);
        body.put("peopleCount", people);
        return body;
    }

    private JsonNode book(long hotel, int people, MockHttpSession volunteer) throws Exception {
        return json.readTree(postJson("/api/v1/me/pickups", pledge(hotel, people), volunteer)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    @Test
    void theHotelSeesTheBookingAndAcceptingMarksTheMealServed() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long hotel = hotelIn(openArea(admin), 500, admin);
        MockHttpSession staff = hotelStaff(hotel, admin);
        MockHttpSession volunteer = registerVolunteer(uniqueEmail());

        JsonNode booking = book(hotel, 12, volunteer);
        long id = booking.get("id").asLong();

        JsonNode open = json.readTree(getAs(staff, "/api/v1/hotel/pickups").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(open.get("content").size()).isEqualTo(1);
        assertThat(open.get("content").get(0).get("peopleCount").asInt()).isEqualTo(12);
        assertThat(open.get("content").get(0).has("platesServed")).isFalse();
        getAs(staff, "/api/v1/hotel/summary").andExpect(status().isOk())
                .andExpect(jsonPath("$.bookedToday").value(12)).andExpect(jsonPath("$.waiting").value(1))
                .andExpect(jsonPath("$.platesServed").doesNotExist());

        postJson("/api/v1/hotel/pickups/" + id + "/accept", Map.of(), staff)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.closedBy").value("HOTEL"));

        getAs(volunteer, "/api/v1/me/summary").andExpect(jsonPath("$.deliveries").value(1)).andExpect(jsonPath("$.platesServed").value(12));
        postJson("/api/v1/hotel/pickups/" + id + "/accept", Map.of(), staff).andExpect(status().isConflict());
    }

    @Test
    void theAdminCanAcceptOnTheHotelsBehalfAndTheHotelSeesItIsDone() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long hotel = hotelIn(openArea(admin), 500, admin);
        MockHttpSession staff = hotelStaff(hotel, admin);
        MockHttpSession volunteer = registerVolunteer(uniqueEmail());
        long id = book(hotel, 8, volunteer).get("id").asLong();

        postJson("/api/v1/pickups/" + id + "/accept", Map.of(), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.closedBy").value("ADMIN")).andExpect(jsonPath("$.platesServed").value(8));
        JsonNode open = json.readTree(getAs(staff, "/api/v1/hotel/pickups").andReturn().getResponse().getContentAsString());
        assertThat(open.get("content").size()).isEqualTo(0);
        JsonNode all = json.readTree(getAs(staff, "/api/v1/hotel/pickups?open=false").andReturn().getResponse().getContentAsString());
        assertThat(all.get("content").get(0).get("closedBy").asText()).isEqualTo("ADMIN");
    }

    @Test
    void aHotelOnlyTouchesItsOwnBookings() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long area = openArea(admin);
        long mine = hotelIn(area, 500, admin);
        long other = hotelIn(area, 500, admin);
        MockHttpSession staff = hotelStaff(mine, admin);
        MockHttpSession volunteer = registerVolunteer(uniqueEmail());
        JsonNode booking = book(other, 5, volunteer);

        postJson("/api/v1/hotel/pickups/" + booking.get("id").asLong() + "/accept", Map.of(), staff).andExpect(status().isNotFound());
        getAs(staff, "/api/v1/hotel/summary").andExpect(jsonPath("$.waiting").value(0));
    }

    @Test
    void rolesAreKeptApart() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long hotel = hotelIn(openArea(admin), 500, admin);
        MockHttpSession staff = hotelStaff(hotel, admin);
        MockHttpSession volunteer = registerVolunteer(uniqueEmail());

        getAs(volunteer, "/api/v1/hotel/summary").andExpect(status().isForbidden());
        getAs(admin, "/api/v1/hotel/summary").andExpect(status().isForbidden());
        getAs(null, "/api/v1/hotel/summary").andExpect(status().isUnauthorized());
        getAs(staff, "/api/v1/dashboard").andExpect(status().isForbidden());
        getAs(staff, "/api/v1/volunteers").andExpect(status().isForbidden());
        postJson("/api/v1/me/pickups", pledge(hotel, 3), staff).andExpect(status().isForbidden());
        getAs(staff, "/api/v1/auth/me").andExpect(jsonPath("$.role").value("HOTEL"));
        postJson("/api/v1/me/password", Map.of("currentPassword", PASSWORD, "newPassword", "Another-good-pass-7"), staff)
                .andExpect(status().isNoContent());
    }

    @Test
    void theDailyLimitCountsPlatesAndCancellingGivesThemBack() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long area = openArea(admin);
        long hotel = hotelIn(area, 5, admin);   // this hotel gives 5 plates a day

        MockHttpSession a = registerVolunteer(uniqueEmail());
        MockHttpSession b = registerVolunteer(uniqueEmail());
        MockHttpSession c = registerVolunteer(uniqueEmail());
        book(hotel, 3, a);
        long second = book(hotel, 2, b).get("id").asLong();

        assertThat(spotsLeft(area)).isEqualTo(0);
        postJson("/api/v1/me/pickups", pledge(hotel, 1), c).andExpect(status().isConflict());

        postJson("/api/v1/me/pickups/" + second + "/cancel", Map.of(), b).andExpect(status().isOk());
        assertThat(spotsLeft(area)).isEqualTo(2);
        // two plates are left, so three is too many, and two is fine
        postJson("/api/v1/me/pickups", pledge(hotel, 3), c).andExpect(status().isConflict());
        book(hotel, 2, c);
        assertThat(spotsLeft(area)).isEqualTo(0);
    }

    private int spotsLeft(long area) throws Exception {
        String hotels = getAs(null, "/api/v1/public/areas/" + area + "/hotels").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(hotels).get(0).get("spotsLeft").asInt();
    }

    @Test
    void theAdminCanTakeHotelAccessAwayAgain() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long hotel = hotelIn(openArea(admin), 500, admin);
        String email = uniqueEmail();
        registerVolunteer(email);
        long id = volunteerId(email, admin);
        postJson("/api/v1/volunteers/" + id + "/make-hotel", Map.of("vendorId", hotel), admin).andExpect(status().isOk());
        postJson("/api/v1/volunteers/" + id + "/make-volunteer", Map.of(), admin).andExpect(status().isOk());
        getAs(login(email, PASSWORD), "/api/v1/auth/me").andExpect(jsonPath("$.role").value("VOLUNTEER"));
    }
}
