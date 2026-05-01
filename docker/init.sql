-- MindCare - Database Initialization
-- Automatically executed on first MySQL container startup

-- Create database for Lost Item Service
CREATE DATABASE IF NOT EXISTS lost_item_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Grant all privileges to root from any host
GRANT ALL PRIVILEGES ON lost_item_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%';
FLUSH PRIVILEGES;
