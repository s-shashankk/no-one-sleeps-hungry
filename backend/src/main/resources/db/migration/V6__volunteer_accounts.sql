-- Volunteers sign up with email + password. Everyone who already exists in `users` is an admin.
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'ADMIN';   -- ADMIN or VOLUNTEER

-- A pickup now records how many people the volunteer says they will feed, and which account made it.
-- Older rows (and sample data) keep volunteer_user_id empty and a people count of 1.
ALTER TABLE pickups
    ADD COLUMN people_count INT NOT NULL DEFAULT 1,
    ADD COLUMN volunteer_user_id BIGINT NULL,
    ADD INDEX idx_pickups_volunteer (volunteer_user_id),
    ADD CONSTRAINT fk_pickups_volunteer_user FOREIGN KEY (volunteer_user_id) REFERENCES users (id),
    ADD CONSTRAINT chk_pickup_people CHECK (people_count > 0);
