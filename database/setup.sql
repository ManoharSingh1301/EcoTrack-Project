-- ============================================================================
-- EcoTrack Database Setup
-- ----------------------------------------------------------------------------
-- The consolidated backend uses a SINGLE MySQL database named `ecotrack`.
-- Hibernate (spring.jpa.hibernate.ddl-auto=update) creates every table and the
-- performance indexes (declared via JPA @Index annotations) automatically on
-- first startup, so you normally only need to create the database itself.
--
-- Run this file once with a privileged MySQL account:
--     mysql -u root -p < database/setup.sql
-- ============================================================================

-- 1. Create the application database (utf8mb4 for full Unicode / emoji support).
CREATE DATABASE IF NOT EXISTS ecotrack
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. (Optional) Create a dedicated application user instead of using root.
--    Uncomment and set a strong password, then point DB_USERNAME/DB_PASSWORD
--    in backend/.env at these credentials.
--
-- CREATE USER IF NOT EXISTS 'ecotrack'@'localhost' IDENTIFIED BY 'change_me';
-- GRANT ALL PRIVILEGES ON ecotrack.* TO 'ecotrack'@'localhost';
-- FLUSH PRIVILEGES;

SHOW DATABASES LIKE 'ecotrack';

-- Tables created automatically by JPA on first backend startup:
--   users            — accounts (BCrypt-hashed passwords)
--   items            — shareable items (+ image bytes, borrow metadata)
--   borrow_requests  — borrow workflow (pending/accepted/returned/…)
--   favorites        — per-user wishlist
--   chat_messages    — persisted chat history
