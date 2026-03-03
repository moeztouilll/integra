-- ============================================
-- Gamification System Tables
-- WARNING: This will delete all existing gamification data!
-- ============================================

USE 3a8;

-- Drop tables in correct order (respecting foreign key constraints)
DROP TABLE IF EXISTS `user_badge`;
DROP TABLE IF EXISTS `badge`;
DROP TABLE IF EXISTS `user_points`;
DROP TABLE IF EXISTS `quiz_attempt`;
DROP TABLE IF EXISTS `quiz_answer`;
DROP TABLE IF EXISTS `quiz_question`;
DROP TABLE IF EXISTS `quiz_courses`;
DROP TABLE IF EXISTS `quiz`;
DROP TABLE IF EXISTS `course_reports`;
DROP TABLE IF EXISTS `course_interactions`;
DROP TABLE IF EXISTS `course_history`;
DROP TABLE IF EXISTS `course`;

-- ============================================
-- Course Table
-- ============================================
CREATE TABLE `course` (
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
    FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX idx_course_category (category),
    INDEX idx_course_difficulty (difficulty),
    INDEX idx_course_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Course History Table
-- ============================================
CREATE TABLE `course_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` CHAR(36) NOT NULL,
    `course_id` BIGINT NOT NULL,
    `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `completed_at` TIMESTAMP NULL,
    `progress_percentage` INT DEFAULT 0,
    `last_accessed` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE,
    UNIQUE KEY unique_user_course (user_id, course_id),
    INDEX idx_history_user (user_id),
    INDEX idx_history_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Course Interactions Table (Likes/Dislikes)
-- ============================================
CREATE TABLE `course_interactions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` CHAR(36) NOT NULL,
    `course_id` BIGINT NOT NULL,
    `interaction_type` ENUM('like', 'dislike') NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE,
    UNIQUE KEY unique_user_course_interaction (user_id, course_id),
    INDEX idx_interaction_user (user_id),
    INDEX idx_interaction_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Course Reports Table
-- ============================================
CREATE TABLE `course_reports` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_id` BIGINT NOT NULL,
    `user_id` CHAR(36) NOT NULL,
    `reason` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `status` ENUM('pending', 'reviewed', 'resolved', 'dismissed') DEFAULT 'pending',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `reviewed_at` TIMESTAMP NULL,
    `reviewed_by` CHAR(36),
    FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`reviewed_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX idx_report_course (course_id),
    INDEX idx_report_user (user_id),
    INDEX idx_report_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Quiz Table
-- ============================================
CREATE TABLE `quiz` (
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
    FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX idx_quiz_difficulty (difficulty),
    INDEX idx_quiz_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Quiz Questions Table
-- ============================================
CREATE TABLE `quiz_question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `quiz_id` BIGINT NOT NULL,
    `question_text` TEXT NOT NULL,
    `question_type` ENUM('multiple_choice', 'true_false', 'short_answer') DEFAULT 'multiple_choice',
    `points` INT DEFAULT 1,
    `order_index` INT DEFAULT 0,
    FOREIGN KEY (`quiz_id`) REFERENCES `quiz`(`id`) ON DELETE CASCADE,
    INDEX idx_question_quiz (quiz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Quiz Answers Table
-- ============================================
CREATE TABLE `quiz_answer` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `question_id` BIGINT NOT NULL,
    `answer_text` TEXT NOT NULL,
    `is_correct` BOOLEAN DEFAULT FALSE,
    `order_index` INT DEFAULT 0,
    FOREIGN KEY (`question_id`) REFERENCES `quiz_question`(`id`) ON DELETE CASCADE,
    INDEX idx_answer_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Quiz Attempts Table
-- ============================================
CREATE TABLE `quiz_attempt` (
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
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`quiz_id`) REFERENCES `quiz`(`id`) ON DELETE CASCADE,
    INDEX idx_attempt_user (user_id),
    INDEX idx_attempt_quiz (quiz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Quiz Courses Junction Table
-- ============================================
CREATE TABLE `quiz_courses` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `quiz_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    FOREIGN KEY (`quiz_id`) REFERENCES `quiz`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE,
    UNIQUE KEY unique_quiz_course (quiz_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Badge Table
-- ============================================
CREATE TABLE `badge` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `icon_url` VARCHAR(500),
    `points_required` INT DEFAULT 0,
    `badge_type` ENUM('achievement', 'milestone', 'special') DEFAULT 'achievement',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_badge_type (badge_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- User Badges Table
-- ============================================
CREATE TABLE `user_badge` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` CHAR(36) NOT NULL,
    `badge_id` BIGINT NOT NULL,
    `earned_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`badge_id`) REFERENCES `badge`(`id`) ON DELETE CASCADE,
    UNIQUE KEY unique_user_badge (user_id, badge_id),
    INDEX idx_user_badge_user (user_id),
    INDEX idx_user_badge_badge (badge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- User Points Table
-- ============================================
CREATE TABLE `user_points` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` CHAR(36) NOT NULL,
    `points` INT DEFAULT 0,
    `level` INT DEFAULT 1,
    `total_courses_completed` INT DEFAULT 0,
    `total_quizzes_passed` INT DEFAULT 0,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    UNIQUE KEY unique_user_points (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Insert Sample Data
-- ============================================

-- Sample Courses
INSERT INTO `course` (`title`, `description`, `category`, `difficulty`, `duration_minutes`, `content`, `points_reward`) VALUES
('Introduction to Java', 'Learn the basics of Java programming', 'programming', 'beginner', 120, 'Java is a popular programming language...', 50),
('Advanced SQL Queries', 'Master complex SQL queries and optimization', 'database', 'advanced', 180, 'SQL is essential for database management...', 100),
('Web Development Fundamentals', 'HTML, CSS, and JavaScript basics', 'web', 'beginner', 150, 'Web development is the foundation...', 60);

-- Sample Badges
INSERT INTO `badge` (`name`, `description`, `points_required`, `badge_type`) VALUES
('First Steps', 'Complete your first course', 50, 'achievement'),
('Knowledge Seeker', 'Complete 5 courses', 250, 'milestone'),
('Quiz Master', 'Pass 10 quizzes', 200, 'achievement'),
('Expert Learner', 'Reach level 10', 1000, 'milestone');
