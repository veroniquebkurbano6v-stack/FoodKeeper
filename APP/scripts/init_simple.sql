-- ============================================
-- 食库管家数据库初始化脚本
-- ============================================

CREATE TABLE IF NOT EXISTS category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL,
    sort INT DEFAULT 0,
    parent_id BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS food (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    category_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    unit VARCHAR(20) NOT NULL,
    purchase_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    storage_zone VARCHAR(20) NOT NULL,
    price DECIMAL(18, 2) DEFAULT 0.00,
    total_price DECIMAL(18, 2) DEFAULT 0.00,
    status TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_category_id (category_id),
    INDEX idx_expiry_date (expiry_date),
    INDEX idx_status (status),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    avatar VARCHAR(255) DEFAULT NULL,
    gender TINYINT DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    last_login_time DATETIME DEFAULT NULL,
    last_login_ip VARCHAR(50) DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS outbound_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    food_id BIGINT NOT NULL,
    food_name VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    outbound_type VARCHAR(20) NOT NULL,
    reason VARCHAR(50) DEFAULT NULL,
    remaining_quantity INT DEFAULT NULL,
    operator_id BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_food_id (food_id),
    INDEX idx_outbound_type (outbound_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS preference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    dietary_restrictions VARCHAR(500) DEFAULT NULL,
    preferred_cuisine VARCHAR(500) DEFAULT NULL,
    spiciness VARCHAR(20) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO category (id, name, code, sort, parent_id) VALUES
(1, '蔬菜', 'vegetable', 1, NULL),
(2, '肉类', 'meat', 2, NULL),
(3, '海鲜', 'seafood', 3, NULL),
(4, '干货', 'dried', 4, NULL),
(5, '冷冻', 'frozen', 5, NULL),
(6, '调料', 'seasoning', 6, NULL),
(7, '水果', 'fruit', 7, NULL),
(8, '饮品', 'beverage', 8, NULL);

INSERT IGNORE INTO sys_user (id, username, password, nickname, phone, email, gender, status) VALUES
(1, 'admin', '$2a$10$7EqJtq98hPqEX7fNZaFWoO1oE3vV1eH9y6rT5u4I3O2P1N0M9L8K7J6I5H4G3F2E1D0C9B8A7', '超级管理员', '13800138000', 'admin@food.com', 1, 1);