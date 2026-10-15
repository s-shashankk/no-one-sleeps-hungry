package com.mealup.common.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** The database stores UTC as LocalDateTime; the API speaks Instant (ISO-8601 with Z). */
public final class Utc {

    private Utc() { }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static Instant toInstant(LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }

    public static LocalDateTime fromInstant(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
