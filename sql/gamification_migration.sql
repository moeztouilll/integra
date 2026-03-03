-- ============================================
-- Gamification System Migration
-- Safely updates existing tables or creates new ones
-- ============================================

USE 3a8;

-- ============================================
-- Check and Update Course Table
-- ============================================

-- Drop existing course table if you want to start fresh (CAUTION: This deletes data!)
-- Uncomment the next line only if you want to recreate the table from scratch
-- DROP TABLE IF EXISTS `course_reports`, `course_interactions`, `course_history`, `quiz_courses`, `course`;

-- Create course table with all required columns
CREATE TABLE IF NOT EXISTS `course` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `category` VARCHAR(100),
    `difficulty` ENUM('beginner', 'intermediate', 'advanced', 'expert') DEFAULT 'beginner',
    `duration_minutes` INT DEFAULT 0,
    `thumbnail_url` VARCHAR(500),
    `video_url` VARCHAR(500),
    `content` LONGTEXT,
    `points_reward` INT DEFAULT 10,
    `created_by` CHAR(36),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_active` BOOLEAN DEFAULT TRUE,
    `view_count` INT DEFAULT 0,
    `like_count` INT DEFAULT 0,
    `dislike_count` INT DEFAULT 0,
    INDEX idx_course_category (category),
    INDEX idx_course_difficulty (difficulty),
    INDEX idx_course_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add missing columns if table already exists
-- These will fail silently if columns already exist

-- Add difficulty column
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = '3a8' AND TABLE_NAME = 'course' AND COLUMN_NAME = 'difficulty') = 0,
    'ALTER TABLE `course` ADD COLUMN `difficulty` ENUM(''beginner'', ''intermediate'', ''advanced'', ''expert'') DEFAULT ''beginner'' AFTER `category`',
    'SELECT ''Column difficulty already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add duration_minutes column
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = '3a8' AND TABLE_NAME = 'course' AND COLUMN_NAME = 'duration_minutes') = 0,
    'ALTER TABLE `course` ADD COLUMN `duration_minutes` INT DEFAULT 0 AFTER `difficulty`',
    'SELECT ''Column duration_minutes already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add thumbnail_url column
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = '3a8' AND TABLE_NAME = 'course' AND COLUMN_NAME = 'thumbnail_url') = 0,
    'ALTER TABLE `course` ADD COLUMN `thumbnail_url` VARCHAR(500) AFTER `duration_minutes`',
    'SELECT ''Column thumbnail_url already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add video_url column
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = '3a8' AND TABLE_NAME = 'course' AND COLUMN_NAME = 'video_url') = 0,
    'ALTER TABLE `course` ADD COLUMN `video_url` VARCHAR(500) AFTER `thumbnail_url`',
    'SELECT ''Column video_url already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add points_reward column
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = '3a8' AND TABLE_NAME = 'course' AND COLUMN_NAME = 'points_reward') = 0,
    'ALTER TABLE `course` ADD COLUMN `points_reward` INT DEFAULT 10 AFTER `content`',
    'SELECT ''Column points_reward already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add created_by column
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = '3a8' AND TABLE_NAME = 'course' AND COLUMN_NAME = 'created_by') = 0,
    'ALTER TABLE `course` ADD COLUMN `created_by` CHAR(36) AFTER `points_reward`',
    'SELECT ''Column created_by already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add is_active column
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = '3a8' AND TABLE_NAME = 'course' AND COLUMN_NAME = 'is_active') = 0,
    'ALTER TABLE `course` ADD COLUMN `is_active` BOOLEAN DEFAULT TRUE AFTER `updated_at`',
    'SELECT ''Column is_active already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add view_count column
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = '3a8' AND TABLE_NAME = 'course' AND COLUMN_NAME = 'view_count') = 0,
    'ALTER TABLE `course` ADD COLUMN `view_count` INT DEFAULT 0 AFTER `is_active`',
    'SELECT ''Column view_count already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add like_count column
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = '3a8' AND TABLE_NAME = 'course' AND COLUMN_NAME = 'like_count') = 0,
    'ALTER TABLE `course` ADD COLUMN `like_count` INT DEFAULT 0 AFTER `view_count`',
    'SELECT ''Column like_count already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add dislike_count column
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = '3a8' AND TABLE_NAME = 'course' AND COLUMN_NAME = 'dislike_count') = 0,
    'ALTER TABLE `course` ADD COLUMN `dislike_count` INT DEFAULT 0 AFTER `like_count`',
    'SELECT ''Column dislike_count already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- Create Related Tables
-- ============================================

CREATE TABLE IF NOT EXISTS `course_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` CHAR(36) NOT NULL,
    `course_id` BIGINT NOT NULL,
    `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `completed_at` TIMESTAMP NULL,
    `progress_percentage` INT DEFAULT 0,
    `last_accessed` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_course (user_id, course_id),
    INDEX idx_history_user (user_id),
    INDEX idx_history_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_interactions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` CHAR(36) NOT NULL,
    `course_id` BIGINT NOT NULL,
    `interaction_type` ENUM('like', 'dislike') NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_course_interaction (user_id, course_id),
    INDEX idx_interaction_user (user_id),
    INDEX idx_interaction_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_reports` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_id` BIGINT NOT NULL,
    `user_id` CHAR(36) NOT NULL,
    `reason` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `status` ENUM('pending', 'reviewed', 'resolved', 'dismissed') DEFAULT 'pending',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `reviewed_at` TIMESTAMP NULL,
    `reviewed_by` CHAR(36),
    INDEX idx_report_course (course_id),
    INDEX idx_report_user (user_id),
    INDEX idx_report_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `difficulty` ENUM('beginner', 'intermediate', 'advanced', 'expert') DEFAULT 'beginner',
    `time_limit_minutes` INT DEFAULT 30,
    `passing_score` INT DEFAULT 70,
    `points_reward` INT DEFAULT 20,
    `created_by` CHAR(36),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_active` BOOLEAN DEFAULT TRUE,
    INDEX idx_quiz_difficulty (difficulty),
    INDEX idx_quiz_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `quiz_id` BIGINT NOT NULL,
    `question_text` TEXT NOT NULL,
    `question_type` ENUM('multiple_choice', 'true_false', 'short_answer') DEFAULT 'multiple_choice',
    `points` INT DEFAULT 1,
    `order_index` INT DEFAULT 0,
    INDEX idx_question_quiz (quiz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_answer` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `question_id` BIGINT NOT NULL,
    `answer_text` TEXT NOT NULL,
    `is_correct` BOOLEAN DEFAULT FALSE,
    `order_index` INT DEFAULT 0,
    INDEX idx_answer_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_attempt` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` CHAR(36) NOT NULL,
    `quiz_id` BIGINT NOT NULL,
    `score` INT DEFAULT 0,
    `max_score` INT DEFAULT 0,
    `percentage` DECIMAL(5,2) DEFAULT 0.00,
    `passed` BOOLEAN DEFAULT FALSE,
    `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `completed_at` TIMESTAMP NULL,
    `time_taken_seconds` INT DEFAULT 0,
    INDEX idx_attempt_user (user_id),
    INDEX idx_attempt_quiz (quiz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_courses` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `quiz_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    UNIQUE KEY unique_quiz_course (quiz_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `badge` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `icon_url` VARCHAR(500),
    `points_required` INT DEFAULT 0,
    `badge_type` ENUM('achievement', 'milestone', 'special') DEFAULT 'achievement',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_badge_type (badge_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_badge` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` CHAR(36) NOT NULL,
    `badge_id` BIGINT NOT NULL,
    `earned_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_badge (user_id, badge_id),
    INDEX idx_user_badge_user (user_id),
    INDEX idx_user_badge_badge (badge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_points` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` CHAR(36) NOT NULL,
    `points` INT DEFAULT 0,
    `level` INT DEFAULT 1,
    `total_courses_completed` INT DEFAULT 0,
    `total_quizzes_passed` INT DEFAULT 0,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_points (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Insert Sample Data (only if tables are empty)
-- ============================================

INSERT INTO `course` (`title`, `description`, `category`, `difficulty`, `duration_minutes`, `content`, `points_reward`)
SELECT * FROM (
    SELECT 'Introduction to Java' as title, 'Learn the basics of Java programming' as description, 'programming' as category, 'beginner' as difficulty, 120 as duration_minutes, 'Java is a popular programming language used for building enterprise applications, Android apps, and more. In this course, you will learn variables, data types, control structures, and object-oriented programming concepts.' as content, 50 as points_reward
    UNION ALL
    SELECT 'Advanced SQL Queries', 'Master complex SQL queries and optimization', 'database', 'advanced', 180, 'SQL is essential for database management. This advanced course covers complex joins, subqueries, window functions, query optimization, indexing strategies, and performance tuning.', 100
    UNION ALL
    SELECT 'Web Development Fundamentals', 'HTML, CSS, and JavaScript basics', 'web', 'beginner', 150, 'Web development is the foundation of creating websites and web applications. Learn HTML for structure, CSS for styling, and JavaScript for interactivity.', 60
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `course` LIMIT 1);

INSERT INTO `badge` (`name`, `description`, `points_required`, `badge_type`)
SELECT * FROM (
    SELECT 'First Steps' as name, 'Complete your first course' as description, 50 as points_required, 'achievement' as badge_type
    UNION ALL
    SELECT 'Knowledge Seeker', 'Complete 5 courses', 250, 'milestone'
    UNION ALL
    SELECT 'Quiz Master', 'Pass 10 quizzes', 200, 'achievement'
    UNION ALL
    SELECT 'Expert Learner', 'Reach level 10', 1000, 'milestone'
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `badge` LIMIT 1);

-- Success message
SELECT 'Gamification tables created/updated successfully!' AS Status;
