package com.nosleepshungry.job;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nosleepshungry.model.FoodOffer;
import com.nosleepshungry.repo.FoodOfferRepository;

/** Food that is past its "safe until" time can no longer be collected. */
@Component
public class OfferExpiryJob {

    private final FoodOfferRepository offers;

    public OfferExpiryJob(FoodOfferRepository offers) {
        this.offers = offers;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireOldOffers() {
        offers.expire(FoodOffer.Status.OPEN, FoodOffer.Status.EXPIRED, LocalDateTime.now());
    }
}
