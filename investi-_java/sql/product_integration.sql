-- Product Management Integration
-- Based on Moez's gestion product module

USE 3a8;

-- Ensure users table exists first (should already exist from investi.sql)
-- If not, you need to run investi.sql first

-- Create product table
CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'TND',
    is_digital TINYINT(1) DEFAULT 0,
    download_url VARCHAR(500),
    entrepreneur_id CHAR(36),
    status ENUM('draft','published','archived') DEFAULT 'draft',
    views_count INT DEFAULT 0,
    sales_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    stock INT DEFAULT NULL,
    remise INT DEFAULT NULL,
    category VARCHAR(255) DEFAULT 'Uncategorized',
    INDEX idx_product_status (status),
    INDEX idx_product_category (category),
    INDEX idx_product_entrepreneur (entrepreneur_id),
    FULLTEXT INDEX idx_product_search (name, description, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Product catalog for marketplace functionality';

-- Add foreign key constraint separately (after table creation)
-- This allows the table to be created even if users table doesn't exist yet
ALTER TABLE product 
ADD CONSTRAINT fk_product_entrepreneur 
FOREIGN KEY (entrepreneur_id) REFERENCES users(id) ON DELETE SET NULL;

-- Create sale table
CREATE TABLE IF NOT EXISTS sale (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reference VARCHAR(100) NOT NULL UNIQUE,
    customer_id CHAR(36) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'TND',
    status VARCHAR(50),
    payment_method VARCHAR(100),
    payment_status ENUM('unpaid','paid','refunded') DEFAULT 'unpaid',
    transaction_id VARCHAR(255),
    shipping_address TEXT,
    billing_address TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    product_id BIGINT,
    INDEX idx_sale_customer (customer_id),
    INDEX idx_sale_product (product_id),
    INDEX idx_sale_status (payment_status),
    INDEX idx_sale_reference (reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Sales transactions for products';

-- Add foreign key constraints for sale table
ALTER TABLE sale 
ADD CONSTRAINT fk_sale_customer 
FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE sale 
ADD CONSTRAINT fk_sale_product 
FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE SET NULL;

-- Sample data (optional - from Moez's module)
-- Note: entrepreneur_id should reference existing users in your system
-- Uncomment and adjust IDs as needed:

/*
INSERT INTO product (name, description, price, currency, is_digital, download_url, entrepreneur_id, status, views_count, sales_count, stock, remise, category) VALUES
('Samsung Television', 'Experience vibrant clarity and sharp detail with the Samsung LED Television...', 1400.00, 'TND', 1, NULL, (SELECT id FROM users WHERE role='admin' LIMIT 1), 'published', 0, 0, 5, 20, 'Informatique'),
('Peugeot 208', 'Experience the perfect blend of style, performance, and efficiency...', 20000.00, 'TND', 1, NULL, (SELECT id FROM users WHERE role='admin' LIMIT 1), 'published', 0, 0, 4, 0, 'Voiture'),
('Ordinateur portable HP', 'Experience vibrant performance and sleek design with this modern laptop...', 600.00, 'TND', 1, NULL, (SELECT id FROM users WHERE role='admin' LIMIT 1), 'published', 0, 0, 10, 10, 'Informatique et électronique');
*/
