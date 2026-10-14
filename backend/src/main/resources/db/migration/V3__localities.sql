-- Localities, coordinator assignments, and a locality on every submission.
CREATE TABLE localities (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    city       VARCHAR(120) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_localities_name_city (name, city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE coordinator_localities (
    user_id     BIGINT NOT NULL,
    locality_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, locality_id),
    CONSTRAINT fk_cl_user     FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_cl_locality FOREIGN KEY (locality_id) REFERENCES localities (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE food_offers
    ADD COLUMN locality_id BIGINT NULL,
    ADD CONSTRAINT fk_food_offers_locality FOREIGN KEY (locality_id) REFERENCES localities (id);

ALTER TABLE hunger_reports
    ADD COLUMN locality_id BIGINT NULL,
    ADD CONSTRAINT fk_hunger_reports_locality FOREIGN KEY (locality_id) REFERENCES localities (id);

ALTER TABLE volunteer_applications
    ADD COLUMN locality_id BIGINT NULL,
    ADD CONSTRAINT fk_volunteer_applications_locality FOREIGN KEY (locality_id) REFERENCES localities (id);
