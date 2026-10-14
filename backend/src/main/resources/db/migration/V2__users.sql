-- Admin accounts. The first SUPER_ADMIN is created by the application on first start.
CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    phone         VARCHAR(20)  NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,   -- SUPER_ADMIN / COORDINATOR
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / SUSPENDED
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
