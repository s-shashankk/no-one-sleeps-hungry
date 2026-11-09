package com.mealup.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.mealup.support.ApiTestSupport;

/** Feedback and messages to the admin. */
class FeedbackFlowTest extends ApiTestSupport {

    private Map<String, Object> note(String kind, Integer rating, String body) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", kind);
        if (rating != null) m.put("rating", rating);
        m.put("body", body);
        return m;
    }

    @Test
    void aVolunteerWritesAndTheAdminReadsAndMarksItDone() throws Exception {
        MockHttpSession admin = loginAsFounder();
        String email = uniqueEmail();
        MockHttpSession volunteer = registerVolunteer(email);
        String text = "Unique note " + System.nanoTime();

        postJson("/api/v1/account/messages", note("FEEDBACK", 5, text), volunteer).andExpect(status().isCreated());

        JsonNode mine = json.readTree(getAs(volunteer, "/api/v1/account/messages").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(mine.get("content").get(0).get("body").asText()).isEqualTo(text);

        JsonNode all = json.readTree(getAs(admin, "/api/v1/messages?status=NEW&size=100").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        JsonNode found = null;
        for (JsonNode n : all.get("content")) if (n.get("body").asText().equals(text)) found = n;
        assertThat(found).isNotNull();
        assertThat(found.get("senderEmail").asText()).isEqualTo(email);
        assertThat(found.get("rating").asInt()).isEqualTo(5);

        long id = found.get("id").asLong();
        postJson("/api/v1/messages/" + id + "/done", Map.of(), admin).andExpect(status().isOk());
        postJson("/api/v1/messages/" + id + "/reopen", Map.of(), admin).andExpect(status().isOk());
    }

    @Test
    void ratingsAreBetweenOneAndFiveAndAnEmptyNoteIsRefused() throws Exception {
        MockHttpSession volunteer = registerVolunteer(uniqueEmail());
        postJson("/api/v1/account/messages", note("FEEDBACK", 9, "Too many stars"), volunteer).andExpect(status().isBadRequest());
        postJson("/api/v1/account/messages", note("MESSAGE", null, "   "), volunteer).andExpect(status().isBadRequest());
        postJson("/api/v1/account/messages", note("MESSAGE", null, "Hello team"), volunteer).andExpect(status().isCreated());
    }

    @Test
    void onlySignedInVolunteersAndHotelStaffCanWriteAndOnlyTheAdminCanRead() throws Exception {
        MockHttpSession admin = loginAsFounder();
        MockHttpSession volunteer = registerVolunteer(uniqueEmail());
        postJson("/api/v1/account/messages", note("MESSAGE", null, "Hi"), null).andExpect(status().isUnauthorized());
        postJson("/api/v1/account/messages", note("MESSAGE", null, "Hi"), admin).andExpect(status().isForbidden());
        getAs(volunteer, "/api/v1/messages").andExpect(status().isForbidden());
        getAs(null, "/api/v1/messages").andExpect(status().isUnauthorized());
    }
}
