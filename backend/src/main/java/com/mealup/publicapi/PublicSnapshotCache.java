package com.mealup.publicapi;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.mealup.vendors.LogoChangedEvent;

import com.mealup.publicapi.PublicDtos.Impact;
import com.mealup.publicapi.PublicDtos.PublicArea;
import com.mealup.publicapi.PublicDtos.PublicPartner;

/**
 * Keeps the numbers shown to every visitor (meals served, areas, partner hotels) in memory and rebuilds them
 * once a day, so the home page does not query the database on every visit.
 * The first request after a restart fills it. If a rebuild fails, the last good copy keeps being served.
 * Set mealup.cache.public-enabled=false to read live every time (the tests do this).
 * The live hotel list of an area (places left today) is deliberately NOT cached.
 */
@Component
public class PublicSnapshotCache {

    private static final Logger log = LoggerFactory.getLogger(PublicSnapshotCache.class);

    private record Snapshot(Impact impact, List<PublicArea> areas, List<PublicPartner> partners, Instant builtAt) { }

    private final ImpactService source;
    private final boolean enabled;
    private volatile Snapshot snapshot;

    public PublicSnapshotCache(ImpactService source, @Value("${mealup.cache.public-enabled:true}") boolean enabled) {
        this.source = source;
        this.enabled = enabled;
    }

    public Impact impact() {
        return enabled ? current().impact() : source.impact();
    }

    public List<PublicArea> areas() {
        return enabled ? current().areas() : source.areaList();
    }

    public List<PublicPartner> partners() {
        return enabled ? current().partners() : source.partners();
    }

    /** When the cached copy was built, or null if nothing is cached yet. */
    public Instant builtAt() {
        Snapshot s = snapshot;
        return s == null ? null : s.builtAt();
    }

    /** Rebuilds the cached copy now. Runs every day at the time in mealup.cache.public-refresh-cron (default 00:05 India time). */
    @Scheduled(cron = "${mealup.cache.public-refresh-cron:0 5 0 * * *}", zone = "${mealup.cache.public-refresh-zone:Asia/Kolkata}")
    public void refresh() {
        try {
            snapshot = new Snapshot(source.impact(), source.areaList(), source.partners(), Instant.now());
            log.info("Public numbers refreshed.");
        } catch (RuntimeException e) {
            log.error("Could not refresh the public numbers; keeping the previous copy.", e);
            if (snapshot == null) throw e;
        }
    }

    /** A logo was uploaded or removed: rebuild right after the change is saved, so the home page shows it without waiting for the daily refresh. */
    @TransactionalEventListener(fallbackExecution = true)
    public void onLogoChanged(LogoChangedEvent event) {
        if (!enabled) return;
        try {
            refresh();
        } catch (RuntimeException e) {
            log.warn("Could not refresh the public numbers after a logo change.", e);
        }
    }

    private Snapshot current() {
        Snapshot s = snapshot;
        if (s == null) {
            synchronized (this) {
                if (snapshot == null) refresh();
                s = snapshot;
            }
        }
        return s;
    }
}
