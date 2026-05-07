-- Run in MySQL if `users.role` was created as ENUM with missing values (CAREGIVER, VOLUNTEER).
-- Use database: alzheimer_db
ALTER TABLE users MODIFY COLUMN role VARCHAR(32) NOT NULL;
