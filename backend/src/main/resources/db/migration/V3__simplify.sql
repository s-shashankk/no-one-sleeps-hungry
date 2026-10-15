-- Simplification (Oct 2026): MealUp is now a record-keeping site. Coordination happens on calls and WhatsApp.
-- Four tables remain:  users (admin sign-in),  vendors,  coworkers,  requests.
-- Existing vendors and the founder's login are kept. Volunteers / locality admins become coworkers (no login).

CREATE TABLE coworkers (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(120)  NOT NULL,
    phone_number VARCHAR(20)   NOT NULL,
    location     VARCHAR(300)  NULL,            -- area they cover / live in
    notes        VARCHAR(1000) NULL,
    is_active    TINYINT(1)    NOT NULL DEFAULT 1,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_coworkers_name (name),
    INDEX idx_coworkers_phone (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Everyone who was not the founder becomes a coworker (a phone number and a name are all that is needed now).
INSERT INTO coworkers (name, phone_number, is_active)
SELECT full_name, phone_number, IF(account_status = 'DISABLED', 0, 1) FROM users WHERE role <> 'SUPER_ADMIN';

DROP TABLE delivery_tasks;
DROP TABLE food_allocations;
DROP TABLE food_offers;
DROP TABLE hunger_reports;
DROP TABLE audit_logs;
DROP TABLE invitations;
DROP TABLE admin_localities;

ALTER TABLE vendors DROP FOREIGN KEY fk_vendors_locality, DROP FOREIGN KEY fk_vendors_created_by;
UPDATE vendors SET notes = NULLIF(LEFT(CONCAT_WS(' | ', notes, operating_hours, pickup_instructions), 2000), '');
ALTER TABLE vendors
    DROP COLUMN provider_type,
    DROP COLUMN alternate_phone_number,
    DROP COLUMN locality_id,
    DROP COLUMN latitude,
    DROP COLUMN longitude,
    DROP COLUMN map_link,
    DROP COLUMN preferred_contact,
    DROP COLUMN operating_hours,
    DROP COLUMN pickup_instructions,
    DROP COLUMN verification_status,
    DROP COLUMN created_by_user_id,
    CHANGE COLUMN address location VARCHAR(500) NOT NULL,
    MODIFY COLUMN contact_person VARCHAR(120) NULL;

DROP TABLE localities;

ALTER TABLE users DROP FOREIGN KEY fk_users_created_by;
DELETE FROM users WHERE role <> 'SUPER_ADMIN';
ALTER TABLE users ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1;
UPDATE users SET is_active = IF(account_status = 'ACTIVE', 1, 0);
ALTER TABLE users DROP COLUMN role, DROP COLUMN account_status, DROP COLUMN created_by_user_id;

-- One table for both kinds of request that arrive (from the public forms, or typed in by the admin).
--   NEED_FOOD    people who need food.   PENDING -> COMPLETED (meals_delivered recorded)  or  REJECTED ("unable to serve")
--   SURPLUS_FOOD food on offer.          PENDING -> ACCEPTED (team will collect)          or  REJECTED ("unable to collect")
-- A closed request can be reopened (back to PENDING) to fix a mistake.
CREATE TABLE requests (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type            VARCHAR(20)   NOT NULL,                  -- NEED_FOOD, SURPLUS_FOOD
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',-- PENDING, ACCEPTED, COMPLETED, REJECTED
    contact_name    VARCHAR(120)  NULL,
    contact_phone   VARCHAR(20)   NULL,
    location        VARCHAR(500)  NOT NULL,
    quantity        INT           NOT NULL,                  -- NEED_FOOD: people to feed.  SURPLUS_FOOD: meals on offer.
    details         VARCHAR(1000) NULL,                      -- what food / anything to know
    meals_delivered INT           NULL,                      -- set when a NEED_FOOD request is COMPLETED
    note            VARCHAR(500)  NULL,                      -- admin's note, e.g. why it was rejected
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    closed_at       DATETIME(6)   NULL,
    INDEX idx_requests_status (status, created_at),
    INDEX idx_requests_type (type),
    CONSTRAINT chk_request_quantity CHECK (quantity > 0),
    CONSTRAINT chk_request_meals CHECK (meals_delivered IS NULL OR meals_delivered > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
