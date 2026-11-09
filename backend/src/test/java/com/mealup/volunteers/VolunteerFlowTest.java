package com.mealup.volunteers;

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

/** Volunteer accounts: sign up, sign in, pledge with a number of people, totals, and what the admin sees. */
class VolunteerFlowTest extends ApiTestSupport {

    private long openHotel(MockHttpSession admin) throws Exception {
        long area = createdId("/api/v1/areas",
                Map.of("city", "Testville", "name", "Area-" + UUID.randomUUID().toString().substring(0, 8), "open", true), admin);
        Map<String, Object> hotel = vendorBody();
        hotel.put("areaId", area);
        hotel.put("dailyLimit", 500);
        return createdId("/api/v1/vendors", hotel, admin);
    }

    private Map<String, Object> pledge(long hotel, int people) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vendorId", hotel);
        body.put("peopleCount", people);
        return body;
    }

    private long pledgeId(long hotel, int people, MockHttpSession volunteer) throws Exception {
        return createdId("/api/v1/me/pickups", pledge(hotel, people), volunteer);
    }

    private JsonNode rowFor(String email, MockHttpSession admin) throws Exception {
        String q = email.substring(0, email.indexOf('@'));
        JsonNode page = json.readTree(getAs(admin, "/api/v1/volunteers?q=" + q).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(page.get("content").size()).isEqualTo(1);
        return page.get("content").get(0);
    }

    @Test
    void signingUpCreatesAVolunteerWhoIsSignedInAndCanLogInAgain() throws Exception {
        String email = uniqueEmail();
        MockHttpSession session = registerVolunteer(email);

        getAs(session, "/api/v1/auth/me").andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VOLUNTEER")).andExpect(jsonPath("$.email").value(email));
        postJson("/api/v1/auth/logout", Map.of(), session);
        login(email, PASSWORD);
    }

    @Test
    void signUpRefusesDuplicatesAndWeakInput() throws Exception {
        String email = uniqueEmail();
        registerVolunteer(email);

        Map<String, Object> again = new LinkedHashMap<>();
        again.put("name", "Other"); again.put("email", email.toUpperCase()); again.put("phone", uniquePhone());
        again.put("password", PASSWORD); again.put("consent", true);
        postJson("/api/v1/public/register", again, null).andExpect(status().isConflict());

        Map<String, Object> shortPassword = new LinkedHashMap<>(again);
        shortPassword.put("email", uniqueEmail()); shortPassword.put("password", "short");
        postJson("/api/v1/public/register", shortPassword, null).andExpect(status().isBadRequest());

        Map<String, Object> noConsent = new LinkedHashMap<>(again);
        noConsent.put("email", uniqueEmail()); noConsent.put("consent", false);
        postJson("/api/v1/public/register", noConsent, null).andExpect(status().isBadRequest());

        Map<String, Object> bot = new LinkedHashMap<>(again);
        bot.put("email", uniqueEmail()); bot.put("website", "http://spam.example");
        postJson("/api/v1/public/register", bot, null).andExpect(status().isBadRequest());
    }

    @Test
    void aVolunteerCannotUseAdminPages() throws Exception {
        MockHttpSession volunteer = registerVolunteer(uniqueEmail());
        getAs(volunteer, "/api/v1/dashboard").andExpect(status().isForbidden());
        getAs(volunteer, "/api/v1/volunteers").andExpect(status().isForbidden());
        getAs(volunteer, "/api/v1/pickups").andExpect(status().isForbidden());
        getAs(volunteer, "/api/v1/requests").andExpect(status().isForbidden());
        getAs(loginAsFounder(), "/api/v1/me/summary").andExpect(status().isForbidden());
    }

    @Test
    void totalsFollowDeliveriesAndTheAdminSeesThem() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long hotel = openHotel(admin);
        String email = uniqueEmail();
        MockHttpSession volunteer = registerVolunteer(email);

        long first = pledgeId(hotel, 30, volunteer);
        long second = pledgeId(hotel, 20, volunteer);
        postJson("/api/v1/pickups/" + first + "/complete", Map.of("platesServed", 45), admin).andExpect(status().isOk());

        getAs(volunteer, "/api/v1/me/summary").andExpect(status().isOk())
                .andExpect(jsonPath("$.pickups").value(2)).andExpect(jsonPath("$.deliveries").value(1))
                .andExpect(jsonPath("$.platesServed").value(45)).andExpect(jsonPath("$.peopleReported").value(50))
                .andExpect(jsonPath("$.inProgress").value(1));

        JsonNode row = rowFor(email, admin);
        assertThat(row.get("deliveries").asLong()).isEqualTo(1);
        assertThat(row.get("platesServed").asLong()).isEqualTo(45);
        assertThat(row.get("pickups").asLong()).isEqualTo(2);
        assertThat(row.get("peopleReported").asLong()).isEqualTo(50);

        // the admin can open one volunteer's pickups
        String list = getAs(admin, "/api/v1/volunteers/" + row.get("id").asLong() + "/pickups")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(list).get("totalElements").asLong()).isEqualTo(2);

        // a cancelled pickup no longer counts as people reported
        postJson("/api/v1/me/pickups/" + second + "/cancel", Map.of(), volunteer).andExpect(status().isOk());
        assertThat(rowFor(email, admin).get("peopleReported").asLong()).isEqualTo(30);
    }

    @Test
    void aVolunteerCanHaveOnlyThreePickupsInProgressAndCannotCancelSomeoneElsesPickup() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long hotel = openHotel(admin);
        MockHttpSession volunteer = registerVolunteer(uniqueEmail());
        MockHttpSession other = registerVolunteer(uniqueEmail());

        long mine = pledgeId(hotel, 5, volunteer);
        pledgeId(hotel, 5, volunteer);
        pledgeId(hotel, 5, volunteer);
        postJson("/api/v1/me/pickups", pledge(hotel, 5), volunteer).andExpect(status().isConflict());

        postJson("/api/v1/me/pickups/" + mine + "/cancel", Map.of(), other).andExpect(status().isNotFound());
        postJson("/api/v1/me/pickups/" + mine + "/cancel", Map.of(), volunteer).andExpect(status().isOk());
        postJson("/api/v1/me/pickups", pledge(hotel, 5), volunteer).andExpect(status().isCreated());
    }

    @Test
    void switchingAnAccountOffLocksItOutAndAResetGivesANewPassword() throws Exception {
        MockHttpSession admin = loginAsFounder();
        String email = uniqueEmail();
        MockHttpSession volunteer = registerVolunteer(email);
        long id = rowFor(email, admin).get("id").asLong();

        postJson("/api/v1/volunteers/" + id + "/disable", Map.of(), admin).andExpect(status().isOk());
        getAs(volunteer, "/api/v1/me/summary").andExpect(status().isUnauthorized());
        postJson("/api/v1/auth/login", Map.of("email", email, "password", PASSWORD), null).andExpect(status().isUnauthorized());

        postJson("/api/v1/volunteers/" + id + "/enable", Map.of(), admin).andExpect(status().isOk());
        String temp = json.readTree(postJson("/api/v1/volunteers/" + id + "/reset-password", Map.of(), admin)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("temporaryPassword").asText();
        assertThat(temp).hasSize(12);
        postJson("/api/v1/auth/login", Map.of("email", email, "password", PASSWORD), null).andExpect(status().isUnauthorized());
        MockHttpSession again = login(email, temp);

        postJson("/api/v1/me/password", Map.of("currentPassword", "wrong-password", "newPassword", "A-brand-new-pass-9"), again)
                .andExpect(status().isUnauthorized());
        postJson("/api/v1/me/password", Map.of("currentPassword", temp, "newPassword", "A-brand-new-pass-9"), again)
                .andExpect(status().isNoContent());
        login(email, "A-brand-new-pass-9");
    }

    @Test
    void theAdminCanDownloadAllVolunteersAsCsv() throws Exception {
        MockHttpSession admin = loginAsFounder();
        String email = uniqueEmail();
        registerVolunteer(email);

        String csv = getAs(admin, "/api/v1/volunteers/export.csv").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("Deliveries done").contains(email);
        getAs(null, "/api/v1/volunteers/export.csv").andExpect(status().isUnauthorized());
    }
}
