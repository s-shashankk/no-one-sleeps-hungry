package com.mealup.vendors;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Reads and writes hotel logos (table vendor_logos). Plain SQL: the image bytes are not part of the Vendor entity. */
@Component
public class LogoStore {

    public record Logo(String contentType, byte[] data) { }

    /** Logos are read far more often than they change, so they are kept in memory for a while (and dropped when changed). */
    private static final long KEEP_MILLIS = 6 * 60 * 60 * 1000L;
    private static final int MAX_CACHED = 200;

    private record Cached(Logo logo, long loadedAt) { }

    private final java.util.Map<Long, Cached> cache = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbc;

    public LogoStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(long vendorId, String contentType, byte[] data) {
        jdbc.update("INSERT INTO vendor_logos (vendor_id, content_type, data, version) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE content_type = VALUES(content_type), data = VALUES(data), version = VALUES(version)",
                vendorId, contentType, data, System.currentTimeMillis());
        cache.remove(vendorId);
    }

    public void delete(long vendorId) {
        jdbc.update("DELETE FROM vendor_logos WHERE vendor_id = ?", vendorId);
        cache.remove(vendorId);
    }

    public Optional<Logo> find(long vendorId) {
        long now = System.currentTimeMillis();
        Cached hit = cache.get(vendorId);
        if (hit != null && now - hit.loadedAt() < KEEP_MILLIS) return Optional.of(hit.logo());
        List<Logo> rows = jdbc.query("SELECT content_type, data FROM vendor_logos WHERE vendor_id = ?",
                (rs, i) -> new Logo(rs.getString(1), rs.getBytes(2)), vendorId);
        Optional<Logo> found = rows.stream().findFirst();
        if (found.isPresent()) {
            if (cache.size() >= MAX_CACHED) cache.clear();   // keeps memory bounded
            cache.put(vendorId, new Cached(found.get(), now));
        } else {
            cache.remove(vendorId);
        }
        return found;
    }

    /** The upload time of the logo (used to refresh browser caches), or null when the vendor has no logo. */
    public Long version(long vendorId) {
        List<Long> rows = jdbc.query("SELECT version FROM vendor_logos WHERE vendor_id = ?", (rs, i) -> rs.getLong(1), vendorId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** vendorId -> logo version, only for vendors that have a logo. */
    public Map<Long, Long> versions(Collection<Long> vendorIds) {
        Map<Long, Long> out = new HashMap<>();
        if (vendorIds.isEmpty()) return out;
        String marks = String.join(",", vendorIds.stream().map(x -> "?").toList());
        jdbc.query("SELECT vendor_id, version FROM vendor_logos WHERE vendor_id IN (" + marks + ")",
                rs -> { out.put(rs.getLong(1), rs.getLong(2)); }, vendorIds.toArray());
        return out;
    }
}
