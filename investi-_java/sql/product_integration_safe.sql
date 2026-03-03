-- Product Management Integration (Safe Version)
-- Based on Moez's gestion product module
-- This version creates tables without foreign keys first, then adds them

USE 3a8;

-- Step 1: Create product table WITHOUT foreign key
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

-- Step 2: Create sale table WITHOUT foreign keys
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

-- Step 3: Add foreign key constraints (only if users table exists)
-- If these fail, the tables will still work, just without referential integrity

-- Add product foreign key
SET @fk_exists = (SELECT COUNT(*) 
                  FROM information_schema.TABLE_CONSTRAINTS 
                  WHERE CONSTRAINT_SCHEMA = '3a8' 
                  AND TABLE_NAME = 'product' 
                  AND CONSTRAINT_NAME = 'fk_product_entrepreneur');

SET @sql = IF(@fk_exists = 0,
    'ALTER TABLE product ADD CONSTRAINT fk_product_entrepreneur FOREIGN KEY (entrepreneur_id) REFERENCES users(id) ON DELETE SET NULL',
    'SELECT "Foreign key fk_product_entrepreneur already exists" AS message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add sale foreign keys
SET @fk_exists = (SELECT COUNT(*) 
                  FROM information_schema.TABLE_CONSTRAINTS 
                  WHERE CONSTRAINT_SCHEMA = '3a8' 
                  AND TABLE_NAME = 'sale' 
                  AND CONSTRAINT_NAME = 'fk_sale_customer');

SET @sql = IF(@fk_exists = 0,
    'ALTER TABLE sale ADD CONSTRAINT fk_sale_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE',
    'SELECT "Foreign key fk_sale_customer already exists" AS message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists = (SELECT COUNT(*) 
                  FROM information_schema.TABLE_CONSTRAINTS 
                  WHERE CONSTRAINT_SCHEMA = '3a8' 
                  AND TABLE_NAME = 'sale' 
                  AND CONSTRAINT_NAME = 'fk_sale_product');

SET @sql = IF(@fk_exists = 0,
    'ALTER TABLE sale ADD CONSTRAINT fk_sale_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE SET NULL',
    'SELECT "Foreign key fk_sale_product already exists" AS message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verify tables were created
SELECT 'Product table created successfully' AS status;
SELECT 'Sale table created successfully' AS status;

-- Show table structures
DESCRIBE product;
DESCRIBE sale;

-- Sample data (optional - uncomment to load)
/*
INSERT INTO product (name, description, price, currency, is_digital, download_url, entrepreneur_id, status, views_count, sales_count, stock, remise, category) VALUES
('Samsung Television', 'Experience vibrant clarity and sharp detail with the Samsung LED Television, designed to elevate your viewing experience. Featuring a stunning widescreen display, this TV delivers lifelike images with rich colors and deep contrasts, making every scene come alive. Its sleek black frame adds a modern touch to your living space, while the sturdy stand ensures stability and elegance. Perfect for movies, sports, gaming, and more, this Samsung LED TV transforms your entertainment into a captivating visual journey. Enjoy crisp audio and effortless connectivity options that keep your home entertainment seamless and engaging.', 1400.00, 'TND', 1, NULL, (SELECT id FROM users WHERE role='admin' LIMIT 1), 'published', 0, 0, 5, 20, 'Informatique'),
('Peugeot 208', 'Experience the perfect blend of style, performance, and efficiency with the sleek blue Peugeot 208. This compact hatchback boasts a modern, aerodynamic design highlighted by bold lines and distinctive LED headlights that give it a commanding presence on the road. Equipped with a sophisticated front grille featuring the iconic Peugeot emblem, the 208 exudes elegance and agility. Its sporty alloy wheels and sculpted bodywork enhance its dynamic look, while the spacious interior offers comfort and advanced technology for an effortless driving experience. Whether navigating city streets or cruising on highways, the Peugeot 208 combines practicality with a touch of sophistication—making it an ideal choice for drivers seeking a stylish and reliable vehicle.', 20000.00, 'TND', 1, NULL, (SELECT id FROM users WHERE role='admin' LIMIT 1), 'published', 0, 0, 4, 0, 'Voiture'),
('Ordinateur portable HP', 'Experience vibrant performance and sleek design with this modern laptop. Featuring a crystal-clear display that showcases stunning, vivid colors, it is perfect for both work and entertainment. The lightweight chassis offers portability without sacrificing durability, making it an ideal companion for on-the-go productivity. Equipped with a responsive keyboard and fast processing capabilities, this device ensures smooth multitasking and quick access to your favorite applications. Whether you are tackling professional tasks, streaming your favorite shows, or browsing the web, this laptop delivers reliable performance wrapped in a stylish, contemporary look.', 600.00, 'TND', 1, NULL, (SELECT id FROM users WHERE role='admin' LIMIT 1), 'published', 0, 0, 10, 10, 'Informatique et électronique');
*/
