-- =================================================
-- EcoTrack Database Indexes
-- =================================================
-- Run this script to create performance indexes
-- for the EcoTrack platform.
-- These indexes are also auto-created by JPA @Index
-- annotations when using ddl-auto=update.
-- =================================================

-- =============================================
-- ITEM SERVICE INDEXES (db_items)
-- =============================================
USE db_items;

-- Index on ownerId for filtering items by owner
CREATE INDEX idx_items_owner_id ON items(owner_id);

-- Index on category for category-based filtering
CREATE INDEX idx_items_category ON items(category);

-- Index on available for availability filtering
CREATE INDEX idx_items_available ON items(available);

-- Index on name for search functionality
CREATE INDEX idx_items_name ON items(name);

-- =============================================
-- USER SERVICE INDEXES (db_users)
-- =============================================
USE db_users;

-- Unique index on username for login lookups
CREATE UNIQUE INDEX idx_users_username ON users(username);

-- Unique index on email for duplicate checking
CREATE UNIQUE INDEX idx_users_email ON users(email);

-- =============================================
-- COMMUNICATION SERVICE INDEXES (db_communication)
-- =============================================
USE db_communication;

-- Composite index on senderId + recipientId for chat history queries
CREATE INDEX idx_chat_sender_recipient ON chat_messages(sender_id, recipient_id);

-- Index on itemId for item-specific chat filtering
CREATE INDEX idx_chat_item_id ON chat_messages(item_id);

-- Index on timestamp for ordering chat messages
CREATE INDEX idx_chat_timestamp ON chat_messages(timestamp);
