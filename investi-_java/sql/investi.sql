USE 3a8;

CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role ENUM('admin', 'investor', 'innovator', 'mentor', 'user') NOT NULL,
    avatar_url TEXT,
    bio TEXT,
    points INT DEFAULT 0,
    level INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT FALSE,
    email_verified BOOLEAN DEFAULT FALSE,
    id_image_url TEXT,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role),
    INDEX idx_users_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS evenement (
    id_evenement INT PRIMARY KEY AUTO_INCREMENT,
    id_mentor CHAR(36) NOT NULL,
    titre VARCHAR(200) NOT NULL,
    contenu TEXT NOT NULL,
    lieu VARCHAR(200),
    lieu_latitude DOUBLE,
    lieu_longitude DOUBLE,
    image_url VARCHAR(255),
    date_debut DATETIME NOT NULL,
    date_fin DATETIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_mentor) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT check_dates CHECK (date_fin > date_debut),
    INDEX idx_evenement_mentor (id_mentor),
    INDEX idx_evenement_dates (date_debut, date_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inscription (
    id_inscription INT PRIMARY KEY AUTO_INCREMENT,
    id_user CHAR(36) NOT NULL,
    id_evenement INT NOT NULL,
    date_inscription DATETIME DEFAULT CURRENT_TIMESTAMP,
    statut ENUM('confirme', 'en_attente', 'annule') DEFAULT 'confirme',
    FOREIGN KEY (id_user) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (id_evenement) REFERENCES evenement(id_evenement) ON DELETE CASCADE,
    UNIQUE KEY unique_inscription (id_user, id_evenement),
    INDEX idx_inscription_user (id_user),
    INDEX idx_inscription_event (id_evenement),
    INDEX idx_inscription_status (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


INSERT INTO users (email, password_hash, name, role, is_active, email_verified) 
VALUES ('youssef1timoumi@hotmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Youssef Timoumi', 'admin', TRUE, TRUE);
