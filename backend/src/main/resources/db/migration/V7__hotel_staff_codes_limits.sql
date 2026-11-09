-- Hotel staff sign in (role HOTEL, linked to their hotel); each hotel has a daily booking limit;
-- each booking gets a short code that the volunteer shows and the hotel checks.
ALTER TABLE users ADD COLUMN vendor_id BIGINT NULL,
  ADD INDEX idx_users_vendor (vendor_id),
  ADD CONSTRAINT fk_users_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id);

ALTER TABLE vendors ADD COLUMN daily_limit INT NOT NULL DEFAULT 5,
  ADD CONSTRAINT chk_vendor_daily_limit CHECK (daily_limit BETWEEN 1 AND 500);

ALTER TABLE pickups ADD COLUMN pickup_code VARCHAR(8) NULL, ADD COLUMN closed_by VARCHAR(20) NULL,
  ADD INDEX idx_pickups_vendor_created (vendor_id, created_at);
