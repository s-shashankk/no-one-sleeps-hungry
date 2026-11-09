package com.mealup.pickups;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Frees places that were booked but never collected. Every 15 minutes it cancels bookings still open after
 * mealup.pickups.expire-after-hours (default 12). A cancelled booking no longer counts toward the hotel's daily
 * limit or the volunteer's three-open-pickups limit. Set the hours to 0 to switch it off.
 */
@Component
public class PickupExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(PickupExpiryJob.class);

    private final PickupRepository pickups;
    private final int expireAfterHours;

    public PickupExpiryJob(PickupRepository pickups,
                           @Value("${mealup.pickups.expire-after-hours:12}") int expireAfterHours) {
        this.pickups = pickups;
        this.expireAfterHours = expireAfterHours;
    }

    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT15M")
    public void run() {
        if (expireAfterHours <= 0) return;
        int n = expireCreatedBefore(LocalDateTime.now(ZoneOffset.UTC).minusHours(expireAfterHours));
        if (n > 0) log.info("Closed {} booking(s) that were not collected in {} hours", n, expireAfterHours);
    }

    /** Cancels every open booking created before the cutoff (UTC). Returns how many were closed. */
    @Transactional
    public int expireCreatedBefore(LocalDateTime cutoff) {
        List<Pickup> stale = pickups.findByStatusInAndCreatedAtBefore(
                List.of(PickupStatus.PLEDGED, PickupStatus.CONFIRMED), cutoff);
        stale.forEach(p -> p.cancel("Closed automatically: the food was not collected in time."));
        return stale.size();
    }
}
