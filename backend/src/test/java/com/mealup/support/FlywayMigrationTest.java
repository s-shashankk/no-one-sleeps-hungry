package com.mealup.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

class FlywayMigrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void onlyTheFourSimpleTablesRemain() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class)
                .stream().map(String::toLowerCase).toList();

        assertThat(tables).containsExactlyInAnyOrder("users", "vendors", "coworkers", "requests", "flyway_schema_history");
    }

    @Test
    void emailMustBeUnique() {
        jdbc.update("INSERT INTO users (full_name, phone_number, email) VALUES ('A', '+911111111111', 'dup@test.mealup')");

        org.junit.jupiter.api.Assertions.assertThrows(DuplicateKeyException.class,
                () -> jdbc.update("INSERT INTO users (full_name, phone_number, email) "
                        + "VALUES ('B', '+912222222222', 'dup@test.mealup')"));

        jdbc.update("DELETE FROM users WHERE email = 'dup@test.mealup'");
    }

    @Test
    void aRequestCannotHaveZeroPeopleOrZeroDeliveredMeals() {
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("INSERT INTO requests (type, location, quantity) VALUES ('NEED_FOOD', 'x', 0)"));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("INSERT INTO requests (type, location, quantity, meals_delivered) "
                        + "VALUES ('NEED_FOOD', 'x', 5, 0)"));
    }
}
