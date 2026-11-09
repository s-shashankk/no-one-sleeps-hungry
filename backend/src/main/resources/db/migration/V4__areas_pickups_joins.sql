-- Areas (where MealUp works), hotel pickups promised by volunteers, and requests to join the community.

CREATE TABLE areas (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    city       VARCHAR(100) NOT NULL,
    name       VARCHAR(120) NOT NULL,
    is_open    TINYINT(1)   NOT NULL DEFAULT 0,     -- 1 = volunteers can pick up food here, 0 = coming soon
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_areas_city_name (city, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO areas (city, name, is_open, sort_order) VALUES
    ('Bengaluru', 'Indiranagar', 1, 1),
    ('Bengaluru', 'Vijayanagar', 0, 2),
    ('Bengaluru', 'Domlur',      0, 3);

-- A vendor (hotel) belongs to one area; it is what volunteers see when they choose where to collect food.
ALTER TABLE vendors
    ADD COLUMN area_id BIGINT NULL,
    ADD INDEX idx_vendors_area (area_id),
    ADD CONSTRAINT fk_vendors_area FOREIGN KEY (area_id) REFERENCES areas (id);

-- A volunteer says "I will collect food from this hotel". PLEDGED -> CONFIRMED (admin told the hotel)
-- -> COMPLETED (plates served recorded)  or  CANCELLED.
CREATE TABLE pickups (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id       BIGINT        NOT NULL,
    area_id         BIGINT        NOT NULL,
    volunteer_name  VARCHAR(120)  NOT NULL,
    volunteer_phone VARCHAR(20)   NOT NULL,
    deliver_to      VARCHAR(500)  NULL,                      -- where the volunteer will take the food
    status          VARCHAR(20)   NOT NULL DEFAULT 'PLEDGED',
    plates_served   INT           NULL,                      -- set when COMPLETED
    note            VARCHAR(500)  NULL,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    closed_at       DATETIME(6)   NULL,
    INDEX idx_pickups_status (status, created_at),
    INDEX idx_pickups_area (area_id),
    CONSTRAINT chk_pickup_plates CHECK (plates_served IS NULL OR plates_served > 0),
    CONSTRAINT fk_pickups_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT fk_pickups_area   FOREIGN KEY (area_id)   REFERENCES areas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- "I want to join the MealUp community" (from the public form). NEW -> CONTACTED.
CREATE TABLE join_requests (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(120)  NOT NULL,
    phone_number VARCHAR(20)   NOT NULL,
    email        VARCHAR(150)  NULL,
    location     VARCHAR(200)  NULL,                         -- where they live / can help
    message      VARCHAR(1000) NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'NEW',
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    contacted_at DATETIME(6)   NULL,
    INDEX idx_join_requests_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
