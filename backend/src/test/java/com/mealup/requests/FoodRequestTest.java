package com.mealup.requests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.mealup.common.exception.ConflictException;

/** The life cycle rules, as plain unit tests (no Spring, no Docker). */
class FoodRequestTest {

    private FoodRequest need() {
        return new FoodRequest(RequestType.NEED_FOOD, "Meera", "+919123456789", "Flyover", 20, null);
    }

    private FoodRequest surplus() {
        return new FoodRequest(RequestType.SURPLUS_FOOD, "Wedding Hall", "+919123456780", "Hall Road", 100, null);
    }

    @Test
    void newRequestsArePending() {
        assertThat(need().getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void completingRecordsTheMealsAndClosesTheRequest() {
        FoodRequest r = need();
        r.complete(35, "Served by two coworkers");

        assertThat(r.getStatus()).isEqualTo(RequestStatus.COMPLETED);
        assertThat(r.getMealsDelivered()).isEqualTo(35);
        assertThat(r.getClosedAt()).isNotNull();
    }

    @Test
    void unableToServeRejectsTheRequestWithoutMeals() {
        FoodRequest r = need();
        r.reject("No food available");

        assertThat(r.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(r.getMealsDelivered()).isNull();
    }

    @Test
    void acceptingASurplusOfferSendsTheTeam() {
        FoodRequest r = surplus();
        r.accept(null);

        assertThat(r.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
        assertThat(r.getMealsDelivered()).isNull();
    }

    @Test
    void unableToCollectRejectsASurplusOffer() {
        FoodRequest r = surplus();
        r.reject("No one free to collect");

        assertThat(r.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    void theWrongActionForTheTypeIsRefused() {
        assertThatThrownBy(() -> surplus().complete(10, null)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> need().accept(null)).isInstanceOf(ConflictException.class);
    }

    @Test
    void aClosedRequestCannotBeClosedAgain() {
        FoodRequest r = need();
        r.complete(10, null);

        assertThatThrownBy(() -> r.complete(20, null)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> r.reject(null)).isInstanceOf(ConflictException.class);
    }

    @Test
    void reopeningClearsTheDeliveredMealsAndAllowsAFreshAnswer() {
        FoodRequest r = need();
        r.complete(10, null);

        r.reopen();

        assertThat(r.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(r.getMealsDelivered()).isNull();
        assertThat(r.getClosedAt()).isNull();
        r.complete(12, null);
        assertThat(r.getMealsDelivered()).isEqualTo(12);
    }

    @Test
    void aPendingRequestCannotBeReopened() {
        assertThatThrownBy(() -> need().reopen()).isInstanceOf(ConflictException.class);
    }
}
