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

-- ============================
-- 角色表
-- ============================
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`              BIGINT UNSIGNED NOT NULL                COMMENT '主键',
    `code`            VARCHAR(50)     NOT NULL                COMMENT '角色编码 USER/ADMIN/SUPER_ADMIN',
    `name`            VARCHAR(50)     NOT NULL                COMMENT '角色名',
    `description`     VARCHAR(255)    DEFAULT NULL            COMMENT '说明',
    `sort_order`      INT             NOT NULL DEFAULT 0      COMMENT '排序',
    `status`          TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '1启用/0停用',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ============================
-- 权限表（菜单 + 操作权限）
-- ============================
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`              BIGINT UNSIGNED NOT NULL,
    `parent_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '父权限',
    `code`            VARCHAR(100)    NOT NULL                COMMENT '权限编码 system:user:list',
    `name`            VARCHAR(100)    NOT NULL                COMMENT '名称',
    `type`            TINYINT UNSIGNED NOT NULL               COMMENT '1菜单/2按钮/3API',
    `path`            VARCHAR(255)    DEFAULT NULL            COMMENT '前端路由(类型=1时)',
    `component`       VARCHAR(255)    DEFAULT NULL            COMMENT '前端组件(类型=1时)',
    `icon`            VARCHAR(50)     DEFAULT NULL,
    `sort_order`      INT             NOT NULL DEFAULT 0,
    `status`          TINYINT UNSIGNED NOT NULL DEFAULT 1,
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`, `deleted`),
    KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ============================
-- 用户—角色 关联
-- ============================
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `user_id`         BIGINT UNSIGNED NOT NULL,
    `role_id`         BIGINT UNSIGNED NOT NULL,
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_role`    (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色';

-- ============================
-- 角色—权限 关联
-- ============================
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `role_id`         BIGINT UNSIGNED NOT NULL,
    `permission_id`   BIGINT UNSIGNED NOT NULL,
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`role_id`, `permission_id`),
    KEY `idx_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限';

-- ============================
-- 种子数据：默认角色
-- ============================
INSERT IGNORE INTO `sys_role`(`id`, `code`, `name`, `description`, `sort_order`) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '拥有所有权限', 1),
(2, 'ADMIN',       '管理员',     '平台运营',     2),
(3, 'USER',        '普通用户',   '默认角色',     3);

-- ============================
-- 种子数据：权限节点
-- ============================
INSERT IGNORE INTO `sys_permission`(`id`, `parent_id`, `code`, `name`, `type`, `path`, `sort_order`) VALUES
(1,  0, 'system',             '系统管理',  1, '/system',   1),
(2,  1, 'system:user:list',   '用户列表',  3, NULL,        1),
(3,  1, 'system:user:add',    '新增用户',  3, NULL,        2),
(4,  1, 'system:user:edit',   '编辑用户',  3, NULL,        3),
(5,  1, 'system:user:del',    '删除用户',  3, NULL,        4),
(6,  0, 'business',           '业务管理',  1, '/business',  2),
(7,  6, 'biz:category:list',  '分类列表',  3, NULL,        1),
(8,  6, 'biz:category:add',   '新增分类',  3, NULL,        2),
(9,  6, 'biz:item:list',      '商品列表',  3, NULL,        3),
(10, 6, 'biz:item:audit',     '商品审核',  3, NULL,        4),
(11, 6, 'biz:item:offline',   '强制下架',  3, NULL,        5),
(12, 0, 'auction',            '拍卖',      1, '/auction',   3),
(13,12, 'auction:bid',        '出价',      3, NULL,        1),
(14,12, 'auction:publish',    '发布拍品',  3, NULL,        2);

-- 超管拥有所有权限
INSERT IGNORE INTO `sys_role_permission`(`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission`;

-- 管理员拥有系统管理和业务管理权限
INSERT IGNORE INTO `sys_role_permission`(`role_id`, `permission_id`)
SELECT 2, `id` FROM `sys_permission` WHERE `code` LIKE 'system:%' OR `code` LIKE 'biz:%' OR `code` = 'system' OR `code` = 'business';

-- 普通用户可出价、可发布
INSERT IGNORE INTO `sys_role_permission`(`role_id`, `permission_id`) VALUES
(3, 12), (3, 13), (3, 14);

-- ============================
-- 种子数据：默认管理员账号
-- ============================
INSERT IGNORE INTO `sys_user`(`id`, `username`, `nickname`, `password`, `email`, `status`, `created_at`)
VALUES (1, 'admin', '系统管理员', '$2a$10$N8wB8gD4kZ2hQfvE6rN5/.HpYwvJ7xJiC0bM4Z1WXh1u5VZq3Y1Cy', 'admin@auction.local', 1, NOW());

-- admin 绑定超级管理员角色
INSERT IGNORE INTO `sys_user_role`(`user_id`, `role_id`) VALUES (1, 1);
