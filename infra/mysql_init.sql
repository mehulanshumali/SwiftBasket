-- ============================================================
-- SwiftBasket Microservices - Database Initialization
-- Executed once by the MySQL container on first startup.
-- THIS FILE IS THE SOURCE OF TRUTH FOR ALL SCHEMAS.
-- Services run with spring.jpa.hibernate.ddl-auto: validate
-- and must never create or alter tables themselves.
-- ============================================================

-- ============================================================
-- Databases
-- ============================================================

CREATE DATABASE IF NOT EXISTS user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- User Management (restricted to service user only)
-- ============================================================

CREATE USER IF NOT EXISTS 'swiftbasket_user'@'%' IDENTIFIED BY 'securepass';

GRANT ALL PRIVILEGES ON user_db.* TO 'swiftbasket_user'@'%';
GRANT ALL PRIVILEGES ON product_db.* TO 'swiftbasket_user'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'swiftbasket_user'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'swiftbasket_user'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'swiftbasket_user'@'%';

FLUSH PRIVILEGES;

-- ============================================================
-- user_db
-- ============================================================

USE user_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) DEFAULT NULL,
    address VARCHAR(500) DEFAULT NULL,
    role ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    deleted_at DATETIME(6) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    is_used BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_tokens_token (token),
    KEY idx_password_reset_tokens_user_id (user_id),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- product_db
-- ============================================================

USE product_db;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    image_url VARCHAR(500) DEFAULT NULL,
    category_id BIGINT NOT NULL,
    sku VARCHAR(50) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    active BIT(1) NOT NULL DEFAULT b'1',
    version INT DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_products_sku (sku),
    KEY idx_products_category_id (category_id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- order_db
-- ============================================================

USE order_db;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    status ENUM('PENDING','PAID','PROCESSING','SHIPPED','DELIVERED','PAYMENT_FAILED','CANCELLED') NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    shipping_address VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_orders_user_id_created_at (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order_id (order_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(20) DEFAULT NULL,
    to_status VARCHAR(20) NOT NULL,
    note VARCHAR(255) DEFAULT NULL,
    changed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_status_history_order_id_changed_at (order_id, changed_at),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- payment_db
-- ============================================================

USE payment_db;

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status ENUM('REQUIRES_PAYMENT','PROCESSING','SUCCEEDED','FAILED','REFUNDED') NOT NULL,
    stripe_payment_intent_id VARCHAR(255) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_order_id (order_id),
    UNIQUE KEY uk_payments_stripe_payment_intent_id (stripe_payment_intent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_event_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSON,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_payment_event_log_payment_id_created_at (payment_id, created_at),
    CONSTRAINT fk_payment_event_log_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- notification_db
-- ============================================================

USE notification_db;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_email VARCHAR(255) NOT NULL,
    type ENUM('WELCOME','ORDER_CONFIRMATION','PAYMENT_RECEIPT','ORDER_STATUS_UPDATE') NOT NULL,
    subject VARCHAR(255) DEFAULT NULL,
    content TEXT,
    status ENUM('PENDING','SENT','FAILED') NOT NULL,
    kafka_topic VARCHAR(50) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    sent_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_recipient_email_created_at (recipient_email, created_at),
    KEY idx_notifications_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
