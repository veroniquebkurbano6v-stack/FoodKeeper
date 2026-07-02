-- ============================================
-- 食库管家数据库初始化脚本
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS food_inventory
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE food_inventory;

-- ---------- 分类表 ----------
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `code` VARCHAR(50) NOT NULL COMMENT '分类编码',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父分类ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- ---------- 食材表 ----------
CREATE TABLE IF NOT EXISTS `food` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '食材名称',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `quantity` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    `unit` VARCHAR(20) NOT NULL COMMENT '单位',
    `purchase_date` DATE NOT NULL COMMENT '采购日期',
    `expiry_date` DATE NOT NULL COMMENT '保质期截止',
    `storage_zone` VARCHAR(20) NOT NULL COMMENT '存放分区：refrigerator冷藏室 freezer冷冻层 room常温',
    `price` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '单价',
    `total_price` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '总价',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0正常 1临期 2过期',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除 1已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_expiry_date` (`expiry_date`),
    INDEX `idx_status` (`status`),
    INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材表';

-- ---------- 用户表 ----------
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `gender` TINYINT DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    INDEX `idx_phone` (`phone`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------- 出库日志表 ----------
CREATE TABLE IF NOT EXISTS `outbound_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `food_id` BIGINT NOT NULL COMMENT '食材ID',
    `food_name` VARCHAR(50) NOT NULL COMMENT '食材名称',
    `quantity` INT NOT NULL COMMENT '出库数量',
    `outbound_type` VARCHAR(20) NOT NULL COMMENT '出库类型：cook烹饪消耗 discard变质报废 clear存量清零',
    `reason` VARCHAR(50) DEFAULT NULL COMMENT '原因',
    `remaining_quantity` INT DEFAULT NULL COMMENT '剩余库存',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_food_id` (`food_id`),
    INDEX `idx_outbound_type` (`outbound_type`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库日志表';

-- ---------- 口味偏好表 ----------
CREATE TABLE IF NOT EXISTS `preference` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `dietary_restrictions` VARCHAR(500) DEFAULT NULL COMMENT '忌口食物（JSON数组）',
    `preferred_cuisine` VARCHAR(500) DEFAULT NULL COMMENT '偏好菜系（JSON数组）',
    `spiciness` VARCHAR(20) DEFAULT NULL COMMENT '辣度：mild微辣 medium中辣 hot特辣',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='口味偏好表';

-- ---------- 初始化分类数据 ----------
INSERT INTO `category` (`id`, `name`, `code`, `sort`, `parent_id`) VALUES
(1, '蔬菜', 'vegetable', 1, NULL),
(2, '肉类', 'meat', 2, NULL),
(3, '海鲜', 'seafood', 3, NULL),
(4, '干货', 'dried', 4, NULL),
(5, '冷冻', 'frozen', 5, NULL),
(6, '调料', 'seasoning', 6, NULL),
(7, '水果', 'fruit', 7, NULL),
(8, '饮品', 'beverage', 8, NULL);

-- ---------- 常用食品表 ----------
CREATE TABLE IF NOT EXISTS `favorite_food` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '食材名称',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `unit` VARCHAR(20) NOT NULL COMMENT '单位',
    `storage_zone` VARCHAR(20) NOT NULL COMMENT '存放分区：refrigerator冷藏室 freezer冷冻层 room常温',
    `default_expiry_days` INT DEFAULT NULL COMMENT '默认保质期天数',
    `default_price` DECIMAL(18, 2) DEFAULT NULL COMMENT '默认单价',
    `use_count` INT NOT NULL DEFAULT 1 COMMENT '使用次数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除 1已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_use_count` (`use_count`),
    UNIQUE KEY `uk_user_name` (`user_id`, `name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='常用食品表';

-- ---------- 初始化测试用户 ----------
-- 密码为：123（BCrypt 加密）
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `gender`, `status`) VALUES
(1, 'admin', '$2a$10$IqTJTjn39N6vqNyL6g8S6u7mK6Aq9Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q', '超级管理员', '13800138000', 'admin@food.com', 1, 1);

-- 说明：admin 的密码是 123（简单密码，便于测试）