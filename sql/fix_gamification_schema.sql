-- ============================================================
-- Fix: Align course & course_history tables with Java code
-- Run this script in your MySQL database (USE 3a8;)
-- ============================================================

USE 3a8;

-- ============================================================
-- 1. Add missing columns to the `course` table
-- ============================================================

-- slug (unique URL-friendly identifier)
ALTER TABLE `course`
    ADD COLUMN IF NOT EXISTS `slug` VARCHAR(255) UNIQUE AFTER `title`;

-- content_url (video/content link — maps to Java contentUrl)
ALTER TABLE `course`
    ADD COLUMN IF NOT EXISTS `content_url` VARCHAR(500) AFTER `slug`;

-- content_type (e.g. 'video', 'article')
ALTER TABLE `course`
    ADD COLUMN IF NOT EXISTS `content_type` VARCHAR(50) DEFAULT 'video' AFTER `content_url`;

-- difficulty_level (replaces/aliases the existing `difficulty` column)
ALTER TABLE `course`
    ADD COLUMN IF NOT EXISTS `difficulty_level` VARCHAR(50) DEFAULT 'beginner' AFTER `content_type`;

-- language
ALTER TABLE `course`
    ADD COLUMN IF NOT EXISTS `language` VARCHAR(50) DEFAULT 'French' AFTER `category`;

-- estimated_duration (replaces/aliases `duration_minutes`)
ALTER TABLE `course`
    ADD COLUMN IF NOT EXISTS `estimated_duration` INT DEFAULT 0 AFTER `language`;

-- reward_points (aliases `points_reward`)
ALTER TABLE `course`
    ADD COLUMN IF NOT EXISTS `reward_points` INT DEFAULT 10 AFTER `estimated_duration`;

-- minimum_points_required
ALTER TABLE `course`
    ADD COLUMN IF NOT EXISTS `minimum_points_required` INT DEFAULT 0 AFTER `reward_points`;

-- status (e.g. 'published', 'draft', 'archived')
ALTER TABLE `course`
    ADD COLUMN IF NOT EXISTS `status` ENUM('published','draft','archived') DEFAULT 'published' AFTER `minimum_points_required`;

-- visibility (e.g. 'public', 'private')
ALTER TABLE `course`
    ADD COLUMN IF NOT EXISTS `visibility` ENUM('public','private') DEFAULT 'public' AFTER `status`;

-- published_at
ALTER TABLE `course`
    ADD COLUMN IF NOT EXISTS `published_at` TIMESTAMP NULL AFTER `updated_at`;

-- Copy existing data from old columns into new ones (for rows already in the table)
UPDATE `course` SET
    `content_url`     = COALESCE(`content_url`, `video_url`),
    `difficulty_level`= COALESCE(`difficulty_level`, `difficulty`),
    `estimated_duration` = COALESCE(NULLIF(`estimated_duration`, 0), `duration_minutes`),
    `reward_points`   = COALESCE(NULLIF(`reward_points`, 0), `points_reward`),
    `slug`            = COALESCE(`slug`, REPLACE(LOWER(`title`), ' ', '-'))
WHERE 1=1;

-- ============================================================
-- 2. Add missing columns to `course_history` table
-- ============================================================

-- visited_at (the Java code orders by ch.visited_at)
ALTER TABLE `course_history`
    ADD COLUMN IF NOT EXISTS `visited_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER `course_id`;

-- last_position (used in Java CourseHistory entity)
ALTER TABLE `course_history`
    ADD COLUMN IF NOT EXISTS `last_position` INT DEFAULT 0 AFTER `visited_at`;

-- completion_percentage (used in Java CourseHistory entity)
ALTER TABLE `course_history`
    ADD COLUMN IF NOT EXISTS `completion_percentage` INT DEFAULT 0 AFTER `last_position`;

-- Copy existing progress data if available
UPDATE `course_history` SET
    `visited_at`           = COALESCE(`visited_at`, `last_accessed`),
    `completion_percentage`= COALESCE(NULLIF(`completion_percentage`, 0), `progress_percentage`)
WHERE 1=1;

-- ============================================================
-- 3. Add missing columns to `course_interactions` table
-- ============================================================

-- report_reason (used in Java CourseInteraction entity)
ALTER TABLE `course_interactions`
    ADD COLUMN IF NOT EXISTS `report_reason` VARCHAR(255) NULL AFTER `interaction_type`;

-- Expand interaction_type to support 'report'
ALTER TABLE `course_interactions`
    MODIFY COLUMN `interaction_type` ENUM('like', 'dislike', 'report') NOT NULL;

-- ============================================================
-- 4. Add missing columns to `course_reports` table
-- ============================================================

-- report_reason column (Java uses report_reason, DB has reason)
ALTER TABLE `course_reports`
    ADD COLUMN IF NOT EXISTS `report_reason` VARCHAR(255) NULL AFTER `user_id`;

-- updated_at
ALTER TABLE `course_reports`
    ADD COLUMN IF NOT EXISTS `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;

-- Copy data from reason -> report_reason
UPDATE `course_reports` SET
    `report_reason` = COALESCE(`report_reason`, `reason`)
WHERE 1=1;

-- ============================================================
SELECT 'Schema fix applied successfully!' AS Status;
