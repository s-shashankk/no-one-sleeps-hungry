package com.mealup.requests;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.mealup.support.ApiTestSupport;

/** The whole workflow: a request comes in, the admin decides, the numbers follow. */
class RequestFlowTest extends ApiTestSupport {

    private long pending(MockHttpSession admin) throws Exception {
        return json.readTree(getAs(admin, "/api/v1/dashboard").andReturn().getResponse().getContentAsString())
                .get("pendingRequests").asLong();
    }

    private long meals(MockHttpSession admin) throws Exception {
        return json.readTree(getAs(admin, "/api/v1/dashboard").andReturn().getResponse().getContentAsString())
                .get("totalMealsDelivered").asLong();
    }

    @Test
    void publicFormCreatesAPendingRequestThatOnlyAdminsCanSee() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long before = pending(admin);

        postJson("/api/v1/public/hunger-reports", needFoodBody(25), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.id").doesNotExist());

        org.assertj.core.api.Assertions.assertThat(pending(admin)).isEqualTo(before + 1);
        getAs(null, "/api/v1/requests").andExpect(status().isUnauthorized());
    }

    @Test
    void theFormNeedsConsentALocationAndAPositiveNumber() throws Exception {
        Map<String, Object> noConsent = needFoodBody(5);
        noConsent.put("consent", false);
        Map<String, Object> noLocation = needFoodBody(5);
        noLocation.put("location", "");

        postJson("/api/v1/public/hunger-reports", noConsent, null).andExpect(status().isBadRequest());
        postJson("/api/v1/public/hunger-reports", noLocation, null).andExpect(status().isBadRequest());
        postJson("/api/v1/public/hunger-reports", needFoodBody(0), null).andExpect(status().isBadRequest());
        postJson("/api/v1/public/surplus-food-reports", Map.of("name", "x", "consent", true), null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void aBotThatFillsTheHiddenFieldGetsThanksButNothingIsStored() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long before = pending(admin);
        Map<String, Object> body = needFoodBody(9);
        body.put("website", "http://spam.example");

        postJson("/api/v1/public/hunger-reports", body, null).andExpect(status().isCreated());

        org.assertj.core.api.Assertions.assertThat(pending(admin)).isEqualTo(before);
    }

    @Test
    void completingAFoodRequestRecordsMealsAndLeavesThePendingList() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long id = submitNeedFood(30, admin);
        long pendingBefore = pending(admin);
        long mealsBefore = meals(admin);

        postJson("/api/v1/requests/" + id + "/complete", Map.of("mealsDelivered", 28, "note", "two trips"), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.mealsDelivered").value(28));

        org.assertj.core.api.Assertions.assertThat(pending(admin)).isEqualTo(pendingBefore - 1);
        org.assertj.core.api.Assertions.assertThat(meals(admin)).isEqualTo(mealsBefore + 28);
    }

    @Test
    void mealsDeliveredMustBeAtLeastOne() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long id = submitNeedFood(10, admin);

        postJson("/api/v1/requests/" + id + "/complete", Map.of("mealsDelivered", 0), admin)
                .andExpect(status().isBadRequest());
        postJson("/api/v1/requests/" + id + "/complete", Map.of(), admin).andExpect(status().isBadRequest());
    }

    @Test
    void unableToServeRemovesTheRequestWithoutAddingMeals() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long id = submitNeedFood(12, admin);
        long mealsBefore = meals(admin);
        long pendingBefore = pending(admin);

        postJson("/api/v1/requests/" + id + "/reject", Map.of("note", "no food today"), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        org.assertj.core.api.Assertions.assertThat(pending(admin)).isEqualTo(pendingBefore - 1);
        org.assertj.core.api.Assertions.assertThat(meals(admin)).isEqualTo(mealsBefore);
        getAs(admin, "/api/v1/requests?status=PENDING&size=100").andExpect(jsonPath("$.content[*].id", not(hasItem((int) id))));
    }

    @Test
    void acceptingSurplusFoodMeansATeamWillCollectItAndNoMealsAreCounted() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long id = submitSurplus(80, admin);
        long mealsBefore = meals(admin);

        postJson("/api/v1/requests/" + id + "/accept", Map.of(), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        org.assertj.core.api.Assertions.assertThat(meals(admin)).isEqualTo(mealsBefore);
    }

    @Test
    void unableToCollectRejectsSurplusFood() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long id = submitSurplus(40, admin);

        postJson("/api/v1/requests/" + id + "/reject", Map.of("note", "no one free"), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void theWrongActionForTheKindOfRequestIsRefused() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long need = submitNeedFood(10, admin);
        long surplus = submitSurplus(10, admin);

        postJson("/api/v1/requests/" + surplus + "/complete", Map.of("mealsDelivered", 5), admin)
                .andExpect(status().isConflict());
        postJson("/api/v1/requests/" + need + "/accept", Map.of(), admin).andExpect(status().isConflict());
    }

    @Test
    void aClosedRequestCannotBeCompletedTwiceAndCanBeReopenedToFixAMistake() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long id = submitNeedFood(15, admin);
        long mealsBefore = meals(admin);
        postJson("/api/v1/requests/" + id + "/complete", Map.of("mealsDelivered", 100), admin).andExpect(status().isOk());

        postJson("/api/v1/requests/" + id + "/complete", Map.of("mealsDelivered", 15), admin)
                .andExpect(status().isConflict());

        postJson("/api/v1/requests/" + id + "/reopen", Map.of(), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.mealsDelivered").doesNotExist());
        org.assertj.core.api.Assertions.assertThat(meals(admin)).isEqualTo(mealsBefore);

        postJson("/api/v1/requests/" + id + "/complete", Map.of("mealsDelivered", 15), admin).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(meals(admin)).isEqualTo(mealsBefore + 15);
    }

    @Test
    void anAdminCanTypeInARequestThatArrivedByPhone() throws Exception {
        MockHttpSession admin = loginAsFounder();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "NEED_FOOD");
        body.put("location", "Bus stand, platform 3");
        body.put("quantity", 18);
        body.put("contactName", "Ravi");

        postJson("/api/v1/requests", body, admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.quantity").value(18));
    }

    @Test
    void detailsCanBeCorrectedWithoutChangingTheStatus() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long id = submitNeedFood(10, admin);
        Map<String, Object> fix = new LinkedHashMap<>();
        fix.put("location", "Corrected place");
        fix.put("quantity", 14);

        putJson("/api/v1/requests/" + id, fix, admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Corrected place"))
                .andExpect(jsonPath("$.quantity").value(14))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void listCanBeFilteredByTypeAndStatus() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long need = submitNeedFood(10, admin);
        long surplus = submitSurplus(10, admin);

        getAs(admin, "/api/v1/requests?type=NEED_FOOD&status=PENDING&size=100")
                .andExpect(jsonPath("$.content[*].id", hasItem((int) need)))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) surplus))));
        getAs(admin, "/api/v1/requests?type=SURPLUS_FOOD&size=100")
                .andExpect(jsonPath("$.content[*].id", hasItem((int) surplus)));
    }

    @Test
    void everyActionNeedsSigningIn() throws Exception {
        postJson("/api/v1/requests/1/complete", Map.of("mealsDelivered", 5), null).andExpect(status().isUnauthorized());
        postJson("/api/v1/requests/1/reject", Map.of(), null).andExpect(status().isUnauthorized());
        getAs(null, "/api/v1/dashboard").andExpect(status().isUnauthorized());
    }
}
