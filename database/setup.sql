-- EcoTrack Database Setup Script
-- Run this script to create the required databases for the EcoTrack platform

-- Create database for Item Service
CREATE DATABASE IF NOT EXISTS db_items;

-- Create database for User Service
CREATE DATABASE IF NOT EXISTS db_users;

-- Create database for Communication Service
CREATE DATABASE IF NOT EXISTS db_communication;

-- Show created databases
SHOW DATABASES;

-- Note: The tables will be automatically created by Spring Boot JPA
-- when you run each microservice for the first time (ddl-auto: update)
