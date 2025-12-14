-- ============================================================
-- SwiftBasket Microservices - Database Initialization
-- ============================================================

-- User Service Database
CREATE DATABASE IF NOT EXISTS user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Product Service Database
CREATE DATABASE IF NOT EXISTS product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Order Service Database
CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Payment Service Database
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- User Management (restricted to service user only)
-- ============================================================

-- Create service user (if not exists)
CREATE USER IF NOT EXISTS 'swiftbasket_user'@'%' IDENTIFIED BY 'securepass';

-- Grant specific database privileges (security best practice)
GRANT ALL PRIVILEGES ON user_db.* TO 'swiftbasket_user'@'%';
GRANT ALL PRIVILEGES ON product_db.* TO 'swiftbasket_user'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'swiftbasket_user'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'swiftbasket_user'@'%';

-- Apply privileges
FLUSH PRIVILEGES;