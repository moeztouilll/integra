USE 3a8;

CREATE TABLE IF NOT EXISTS project (
    project_id INT AUTO_INCREMENT PRIMARY KEY,
    entrepreneur_id CHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    amountRequested DOUBLE NOT NULL,
    equityOffered DOUBLE NOT NULL,
    status VARCHAR(50) DEFAULT 'OPEN',
    project_date DATE,
    category VARCHAR(100) DEFAULT 'Other',
    INDEX idx_project_entrepreneur (entrepreneur_id),
    INDEX idx_project_status (status),
    FOREIGN KEY (entrepreneur_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS investment (
    investment_id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    investor_id CHAR(36) NOT NULL,
    totalAmount DOUBLE NOT NULL,
    durationMonths INT NOT NULL,
    amountPerPeriod DOUBLE NOT NULL,
    equityRequested DOUBLE NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    investment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    progressPercentage INT DEFAULT 0,
    latestProgressLog TEXT DEFAULT NULL,
    paymentMonthsCompleted INT DEFAULT 0,
    lastPaymentDate TIMESTAMP NULL,
    INDEX idx_investment_project (project_id),
    INDEX idx_investment_investor (investor_id),
    INDEX idx_investment_status (status),
    FOREIGN KEY (project_id) REFERENCES project(project_id) ON DELETE CASCADE,
    FOREIGN KEY (investor_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS collaboration (
    id INT AUTO_INCREMENT PRIMARY KEY,
    investment_id INT NOT NULL,
    entrepreneur_id CHAR(36) NOT NULL,
    investor_id CHAR(36) NOT NULL,
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    health_score DOUBLE DEFAULT 100,
    default_probability DOUBLE DEFAULT 0,
    fairness_score DOUBLE DEFAULT 100,
    fairness_status VARCHAR(50) DEFAULT 'BALANCED',
    ideal_equity DOUBLE DEFAULT 0,
    equity_deviation DOUBLE DEFAULT 0,
    INDEX idx_collaboration_investment (investment_id),
    INDEX idx_collaboration_entrepreneur (entrepreneur_id),
    INDEX idx_collaboration_investor (investor_id),
    FOREIGN KEY (investment_id) REFERENCES investment(investment_id) ON DELETE CASCADE,
    FOREIGN KEY (entrepreneur_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (investor_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS collaboration_message (
    id INT AUTO_INCREMENT PRIMARY KEY,
    investment_id INT,
    collaboration_id INT,
    sender_id CHAR(36) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'GENERAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_message_investment (investment_id),
    INDEX idx_message_collaboration (collaboration_id),
    INDEX idx_message_sender (sender_id),
    FOREIGN KEY (collaboration_id) REFERENCES collaboration(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS milestone (
    id INT AUTO_INCREMENT PRIMARY KEY,
    collaboration_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    weight DOUBLE DEFAULT 10.0,
    due_date DATE,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_milestone_collaboration (collaboration_id),
    INDEX idx_milestone_status (status),
    FOREIGN KEY (collaboration_id) REFERENCES collaboration(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
