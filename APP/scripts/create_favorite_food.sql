-- ============================================
-- 常用食品表创建脚本
-- ============================================

USE food_inventory;

-- 创建常用食品表
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

-- 验证表是否创建成功
SELECT 'Table favorite_food created successfully!' AS result;
SHOW CREATE TABLE favorite_food;