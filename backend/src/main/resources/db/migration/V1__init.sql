-- Phase 1 / step 1: tables for the three public forms.
-- Users, vendors, deliveries and localities arrive in later migrations.

CREATE TABLE food_offers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_name     VARCHAR(150) NULL,
    submitter_name  VARCHAR(120) NOT NULL,
    submitter_phone VARCHAR(20)  NOT NULL,
    area            VARCHAR(150) NOT NULL,
    pickup_address  VARCHAR(500) NOT NULL,
    description     VARCHAR(1000) NOT NULL,
    servings        INT          NOT NULL,
    food_type       VARCHAR(20)  NOT NULL,
    cooked_at       DATETIME     NULL,
    expires_at      DATETIME     NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_food_offers_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE hunger_reports (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    area           VARCHAR(150) NOT NULL,
    location_text  VARCHAR(500) NOT NULL,
    latitude       DOUBLE NULL,
    longitude      DOUBLE NULL,
    people_count   INT NOT NULL,
    notes          VARCHAR(1000) NULL,
    reporter_name  VARCHAR(120) NULL,
    reporter_phone VARCHAR(20)  NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_hunger_reports_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE volunteer_applications (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                  VARCHAR(120) NOT NULL,
    phone                 VARCHAR(20)  NOT NULL,
    email                 VARCHAR(150) NULL,
    area                  VARCHAR(150) NOT NULL,
    college_or_occupation VARCHAR(200) NULL,
    message               VARCHAR(1000) NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_volunteer_applications_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
