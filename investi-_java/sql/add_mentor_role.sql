-- Migration script to add 'mentor' role to users table
USE 3a8;

-- Alter the role column to include 'mentor'
ALTER TABLE users 
MODIFY COLUMN role ENUM('admin', 'investor', 'innovator', 'mentor', 'user') NOT NULL;
