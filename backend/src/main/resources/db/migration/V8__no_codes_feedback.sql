-- The booking code is gone: the hotel just accepts the booking.
ALTER TABLE pickups DROP COLUMN pickup_code;

-- Feedback and messages from volunteers and hotel staff to the admin.
CREATE TABLE feedback_messages (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT        NOT NULL,
  kind        VARCHAR(20)   NOT NULL,
  rating      INT           NULL,
  body        VARCHAR(2000) NOT NULL,
  status      VARCHAR(20)   NOT NULL DEFAULT 'NEW',
  created_at  DATETIME(6)   NOT NULL,
  handled_at  DATETIME(6)   NULL,
  INDEX idx_feedback_status (status, created_at),
  INDEX idx_feedback_user (user_id),
  CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT chk_feedback_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5)
);
