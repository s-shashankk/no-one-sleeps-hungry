package com.mealup.volunteers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.mealup.common.time.Utc;
import com.mealup.users.Role;
import com.mealup.volunteers.VolunteerDtos.MySummary;
import com.mealup.volunteers.VolunteerDtos.VolunteerRow;

/** Totals per volunteer, worked out by the database in one query (plain SQL). */
@Component
public class VolunteerStats {

    /** Allowed sort orders (never put user input into the SQL). */
    private static final Map<String, String> SORTS = Map.of(
            "recent", "u.created_at DESC, u.id DESC",
            "deliveries", "deliveries DESC, u.id",
            "plates", "plates DESC, u.id",
            "name", "u.full_name ASC, u.id");

    private static final String TOTALS = "COUNT(p.id) AS pickups, "
            + "COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS deliveries, "
            + "COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN p.plates_served ELSE 0 END), 0) AS plates, "
            + "COALESCE(SUM(CASE WHEN p.status <> 'CANCELLED' THEN p.people_count ELSE 0 END), 0) AS people";

    private final JdbcTemplate jdbc;

    public VolunteerStats(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public MySummary mine(long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*), "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END), 0), "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN plates_served ELSE 0 END), 0), "
                + "COALESCE(SUM(CASE WHEN status <> 'CANCELLED' THEN people_count ELSE 0 END), 0), "
                + "COALESCE(SUM(CASE WHEN status IN ('PLEDGED', 'CONFIRMED') THEN 1 ELSE 0 END), 0) "
                + "FROM pickups WHERE volunteer_user_id = ?",
                (rs, i) -> new MySummary(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)),
                userId);
    }

    public long count(String q, Boolean active) {
        List<Object> args = new ArrayList<>();
        String where = where(q, active, args);
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM users u " + where, Long.class, args.toArray());
        return n == null ? 0 : n;
    }

    public List<VolunteerRow> list(String q, Boolean active, String sort, int page, int size) {
        List<Object> args = new ArrayList<>();
        String where = where(q, active, args);
        String order = SORTS.getOrDefault(sort == null ? "recent" : sort, SORTS.get("recent"));
        args.add(size);
        args.add((long) page * size);
        return jdbc.query(
                "SELECT u.id, u.full_name, u.email, u.phone_number, u.is_active, u.created_at, u.last_login_at, u.role, u.vendor_id, h.name AS hotel_name, " + TOTALS
                + " FROM users u LEFT JOIN pickups p ON p.volunteer_user_id = u.id LEFT JOIN vendors h ON h.id = u.vendor_id " + where
                + " GROUP BY u.id, h.name ORDER BY " + order + " LIMIT ? OFFSET ?",
                (rs, i) -> new VolunteerRow(rs.getLong("id"), rs.getString("full_name"), rs.getString("email"),
                        rs.getString("phone_number"), rs.getBoolean("is_active"),
                        Utc.toInstant(rs.getObject("created_at", LocalDateTime.class)),
                        Utc.toInstant(rs.getObject("last_login_at", LocalDateTime.class)),
                        rs.getLong("pickups"), rs.getLong("deliveries"), rs.getLong("plates"), rs.getLong("people"),
                        Role.valueOf(rs.getString("role")), (Long) rs.getObject("vendor_id"), rs.getString("hotel_name")),
                args.toArray());
    }

    private static String where(String q, Boolean active, List<Object> args) {
        StringBuilder w = new StringBuilder("WHERE u.role IN ('VOLUNTEER', 'HOTEL')");
        if (active != null) {
            w.append(" AND u.is_active = ?");
            args.add(active ? 1 : 0);
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            w.append(" AND (LOWER(u.full_name) LIKE ? OR LOWER(u.email) LIKE ? OR u.phone_number LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
        }
        return w.toString();
    }
}
