package com.mealup.pickups;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/** "Today" for daily booking limits means today in India, not in UTC. Timestamps in the database are UTC. */
public final class BookingDay {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    private BookingDay() { }

    /** Midnight at the start of today in India, as a UTC date-time (the way the database stores times). */
    public static LocalDateTime startUtc() {
        return ZonedDateTime.now(INDIA).toLocalDate().atStartOfDay(INDIA)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
