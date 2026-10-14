-- Deliveries (food offer -> hunger reports) and the activity log.
CREATE TABLE deliveries (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    food_offer_id  BIGINT       NOT NULL,
    locality_id    BIGINT       NOT NULL,
    created_by     VARCHAR(150) NOT NULL,
    volunteer_name VARCHAR(120) NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ASSIGNED',
    picked_up_at   DATETIME NULL,
    delivered_at   DATETIME NULL,
    people_served  INT NULL,
    notes          VARCHAR(1000) NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_deliveries_status (status),
    CONSTRAINT fk_deliveries_offer    FOREIGN KEY (food_offer_id) REFERENCES food_offers (id),
    CONSTRAINT fk_deliveries_locality FOREIGN KEY (locality_id)   REFERENCES localities (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE delivery_reports (
    delivery_id      BIGINT NOT NULL,
    hunger_report_id BIGINT NOT NULL,
    PRIMARY KEY (delivery_id, hunger_report_id),
    CONSTRAINT fk_dr_delivery FOREIGN KEY (delivery_id)      REFERENCES deliveries (id),
    CONSTRAINT fk_dr_report   FOREIGN KEY (hunger_report_id) REFERENCES hunger_reports (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE activity_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor      VARCHAR(150) NOT NULL,
    entity     VARCHAR(40)  NOT NULL,
    entity_id  BIGINT NULL,
    action     VARCHAR(60)  NOT NULL,
    details    VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activity_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
