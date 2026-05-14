-- 创建项目开发数据库，统一使用 utf8mb4 以支持中文和 emoji。
CREATE DATABASE IF NOT EXISTS auction DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE auction;

-- 用户主表，保存登录账号、基础资料、状态和审计字段。
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT UNSIGNED NOT NULL,
    `username` VARCHAR(50) NOT NULL,
    `nickname` VARCHAR(50) NOT NULL,
    `password` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `avatar` VARCHAR(500) DEFAULT NULL,
    `gender` TINYINT UNSIGNED DEFAULT 0,
    `real_name` VARCHAR(50) DEFAULT NULL,
    `id_card_no` VARCHAR(64) DEFAULT NULL,
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1,
    `blacklist_reason` VARCHAR(255) DEFAULT NULL,
    `blacklisted_by` BIGINT UNSIGNED DEFAULT NULL,
    `blacklisted_at` DATETIME DEFAULT NULL,
    `last_login_at` DATETIME DEFAULT NULL,
    `last_login_ip` VARCHAR(50) DEFAULT NULL,
    `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_by` BIGINT UNSIGNED DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT UNSIGNED DEFAULT NULL,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`, `deleted`),
    UNIQUE KEY `uk_email` (`email`, `deleted`),
    UNIQUE KEY `uk_phone` (`phone`, `deleted`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 初始化一个管理员账号，方便开发阶段验证数据库查询链路。
INSERT IGNORE INTO `sys_user`(`id`, `username`, `nickname`, `password`, `email`, `status`, `created_at`)
VALUES (1, 'admin', '系统管理员', '$2a$10$N8wB8gD4kZ2hQfvE6rN5/.HpYwvJ7xJiC0bM4Z1WXh1u5VZq3Y1Cy', 'admin@auction.local', 1, NOW());
