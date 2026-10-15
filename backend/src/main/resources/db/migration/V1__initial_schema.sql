-- MealUp initial schema (Step 1). See docs/PRODUCT_SPEC.md section 6 and PLAN.md section 4.
-- Status/role/type columns are VARCHAR holding enum names; the backend validates the values and transitions.
-- All timestamps are stored in UTC.

CREATE TABLE localities (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    city        VARCHAR(120) NOT NULL,
    state       VARCHAR(120) NOT NULL,
    postal_code VARCHAR(12)  NULL,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_localities_name_city (name, city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name          VARCHAR(120) NOT NULL,
    phone_number       VARCHAR(20)  NOT NULL,
    email              VARCHAR(150) NOT NULL,                  -- login identifier
    password_hash      VARCHAR(100) NULL,                      -- NULL until an invited user sets a password
    role               VARCHAR(20)  NOT NULL,                  -- SUPER_ADMIN, LOCALITY_ADMIN, VOLUNTEER
    account_status     VARCHAR(20)  NOT NULL,                  -- INVITED, ACTIVE, DISABLED
    created_by_user_id BIGINT       NULL,
    created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_login_at      DATETIME(6)  NULL,
    UNIQUE KEY uq_users_email (email),
    UNIQUE KEY uq_users_phone (phone_number),
    CONSTRAINT fk_users_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Which localities an admin / volunteer may work in (restricts all their data access).
CREATE TABLE admin_localities (
    user_id     BIGINT NOT NULL,
    locality_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, locality_id),
    CONSTRAINT fk_admin_localities_user     FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_admin_localities_locality FOREIGN KEY (locality_id) REFERENCES localities (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- One-time invitation links. Only a hash of the token is stored.
CREATE TABLE invitations (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT      NOT NULL,
    token_hash         CHAR(64)    NOT NULL,
    expires_at         DATETIME(6) NOT NULL,
    used_at            DATETIME(6) NULL,
    created_by_user_id BIGINT      NOT NULL,
    created_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_invitations_token (token_hash),
    CONSTRAINT fk_invitations_user       FOREIGN KEY (user_id)            REFERENCES users (id),
    CONSTRAINT fk_invitations_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vendors (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                   VARCHAR(160) NOT NULL,
    provider_type          VARCHAR(30)  NOT NULL,   -- HOTEL, RESTAURANT, CATERER, WEDDING, HOUSEWARMING, PARTY, CORPORATE_EVENT, COMMUNITY_EVENT, OTHER
    contact_person         VARCHAR(120) NOT NULL,
    phone_number           VARCHAR(20)  NOT NULL,
    alternate_phone_number VARCHAR(20)  NULL,
    address                VARCHAR(500) NOT NULL,
    locality_id            BIGINT       NOT NULL,
    latitude               DECIMAL(9,6) NULL,
    longitude              DECIMAL(9,6) NULL,
    map_link               VARCHAR(500) NULL,
    preferred_contact      VARCHAR(200) NULL,       -- preferred contact method / time
    operating_hours        VARCHAR(200) NULL,
    pickup_instructions    VARCHAR(1000) NULL,
    notes                  VARCHAR(2000) NULL,      -- internal
    verification_status    VARCHAR(20)  NOT NULL DEFAULT 'UNVERIFIED',   -- UNVERIFIED, VERIFIED
    is_active              TINYINT(1)   NOT NULL DEFAULT 1,
    created_by_user_id     BIGINT       NOT NULL,
    created_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_vendors_locality (locality_id),
    INDEX idx_vendors_name (name),
    INDEX idx_vendors_phone (phone_number),
    CONSTRAINT fk_vendors_locality   FOREIGN KEY (locality_id)        REFERENCES localities (id),
    CONSTRAINT fk_vendors_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE hunger_reports (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_name          VARCHAR(120) NULL,
    reporter_phone         VARCHAR(20)  NULL,
    location_description   VARCHAR(500) NOT NULL,
    locality_id            BIGINT       NULL,       -- NULL when the reporter was unsure; an admin assigns it
    latitude               DECIMAL(9,6) NULL,
    longitude              DECIMAL(9,6) NULL,
    estimated_people_count INT          NOT NULL,
    urgency                VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',   -- LOW, MEDIUM, HIGH, CRITICAL
    context_notes          VARCHAR(1000) NULL,
    best_contact_time      VARCHAR(120) NULL,
    consent_given          TINYINT(1)   NOT NULL,
    image_reference        VARCHAR(255) NULL,       -- key in protected storage, never a public URL
    status                 VARCHAR(30)  NOT NULL DEFAULT 'PENDING_REVIEW',
    status_reason          VARCHAR(500) NULL,       -- why rejected / duplicate / unable to serve
    internal_notes         VARCHAR(2000) NULL,
    reviewed_by_user_id    BIGINT       NULL,
    reviewed_at            DATETIME(6)  NULL,
    created_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_hunger_reports_status (status),
    INDEX idx_hunger_reports_locality (locality_id),
    CONSTRAINT chk_hunger_people CHECK (estimated_people_count > 0),
    CONSTRAINT fk_hunger_reports_locality    FOREIGN KEY (locality_id)         REFERENCES localities (id),
    CONSTRAINT fk_hunger_reports_reviewed_by FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE food_offers (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id               BIGINT        NULL,      -- NULL only for unreviewed public submissions
    reported_by_user_id     BIGINT        NULL,      -- NULL when submitted publicly
    submitter_name          VARCHAR(120)  NULL,      -- public submissions: who reported it
    submitter_phone         VARCHAR(20)   NULL,
    submitted_provider_name VARCHAR(160)  NULL,
    submitted_provider_type VARCHAR(30)   NULL,
    food_items_or_category  VARCHAR(500)  NOT NULL,
    estimated_quantity      DECIMAL(10,2) NOT NULL,
    confirmed_quantity      DECIMAL(10,2) NULL,      -- set when an admin verifies the offer
    unit                    VARCHAR(20)   NOT NULL,  -- MEALS, PORTIONS, KG, PACKETS, OTHER
    preparation_time        DATETIME(6)   NULL,
    storage_details         VARCHAR(500)  NULL,
    packed_and_ready        TINYINT(1)    NOT NULL DEFAULT 0,
    safe_pickup_deadline    DATETIME(6)   NOT NULL,
    pickup_address          VARCHAR(500)  NOT NULL,
    locality_id             BIGINT        NULL,
    status                  VARCHAR(30)   NOT NULL DEFAULT 'PENDING_REVIEW',
    status_reason           VARCHAR(500)  NULL,      -- why rejected / unsafe / cancelled
    notes                   VARCHAR(1000) NULL,      -- notes from the submitter
    internal_notes          VARCHAR(2000) NULL,
    reviewed_by_user_id     BIGINT        NULL,
    reviewed_at             DATETIME(6)   NULL,
    created_at              DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_food_offers_status_deadline (status, safe_pickup_deadline),
    INDEX idx_food_offers_vendor (vendor_id),
    INDEX idx_food_offers_locality (locality_id),
    CONSTRAINT chk_offer_estimated CHECK (estimated_quantity > 0),
    CONSTRAINT chk_offer_confirmed CHECK (confirmed_quantity IS NULL OR confirmed_quantity > 0),
    CONSTRAINT fk_food_offers_vendor      FOREIGN KEY (vendor_id)           REFERENCES vendors (id),
    CONSTRAINT fk_food_offers_reported_by FOREIGN KEY (reported_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_food_offers_locality    FOREIGN KEY (locality_id)         REFERENCES localities (id),
    CONSTRAINT fk_food_offers_reviewed_by FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Many-to-many link between offers and hunger reports, with the quantity given to each.
-- Deliberately no unique (offer, report) pair: a report can be served from one offer in several allocations.
CREATE TABLE food_allocations (
    id                             BIGINT AUTO_INCREMENT PRIMARY KEY,
    food_offer_id                  BIGINT        NOT NULL,
    hunger_report_id               BIGINT        NOT NULL,
    allocated_quantity             DECIMAL(10,2) NOT NULL,
    status                         VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    allocated_by_user_id           BIGINT        NOT NULL,
    assigned_coordinator_user_id   BIGINT        NULL,
    created_at                     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_allocations_offer (food_offer_id),
    INDEX idx_allocations_report (hunger_report_id),
    INDEX idx_allocations_coordinator (assigned_coordinator_user_id),
    CONSTRAINT chk_allocation_quantity CHECK (allocated_quantity > 0),
    CONSTRAINT fk_allocations_offer       FOREIGN KEY (food_offer_id)                REFERENCES food_offers (id),
    CONSTRAINT fk_allocations_report      FOREIGN KEY (hunger_report_id)             REFERENCES hunger_reports (id),
    CONSTRAINT fk_allocations_allocated   FOREIGN KEY (allocated_by_user_id)         REFERENCES users (id),
    CONSTRAINT fk_allocations_coordinator FOREIGN KEY (assigned_coordinator_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- One row per trip, so retries never overwrite earlier history.
CREATE TABLE delivery_tasks (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    food_allocation_id          BIGINT        NOT NULL,
    assigned_to_user_id         BIGINT        NOT NULL,
    pickup_deadline             DATETIME(6)   NULL,
    delivery_deadline           DATETIME(6)   NULL,
    actual_pickup_time          DATETIME(6)   NULL,
    actual_collected_quantity   DECIMAL(10,2) NULL,
    actual_delivery_time        DATETIME(6)   NULL,
    actual_delivered_quantity   DECIMAL(10,2) NULL,
    remaining_quantity          DECIMAL(10,2) NULL,
    status                      VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    failure_reason              VARCHAR(500)  NULL,
    completion_notes            VARCHAR(1000) NULL,
    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_tasks_allocation (food_allocation_id),
    INDEX idx_tasks_assignee_status (assigned_to_user_id, status),
    CONSTRAINT fk_tasks_allocation FOREIGN KEY (food_allocation_id)  REFERENCES food_allocations (id),
    CONSTRAINT fk_tasks_assignee   FOREIGN KEY (assigned_to_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT       NULL,                -- NULL for system actions (e.g. the expiry job)
    action_type   VARCHAR(60)  NOT NULL,
    entity_type   VARCHAR(40)  NOT NULL,
    entity_id     BIGINT       NULL,
    summary       VARCHAR(500) NOT NULL,
    before_values JSON         NULL,
    after_values  JSON         NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_actor (actor_user_id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
