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

-- ============================
-- 操作日志
-- ============================
CREATE TABLE IF NOT EXISTS `sys_oper_log` (
    `id`              BIGINT UNSIGNED NOT NULL,
    `trace_id`        VARCHAR(64)     DEFAULT NULL            COMMENT '链路ID',
    `module`          VARCHAR(50)     NOT NULL                COMMENT '模块名',
    `business_type`   VARCHAR(50)     NOT NULL                COMMENT '业务类型 NEW/EDIT/DELETE...',
    `description`     VARCHAR(255)    DEFAULT NULL,
    `method`          VARCHAR(255)    NOT NULL                COMMENT '调用方法',
    `request_url`     VARCHAR(500)    NOT NULL,
    `request_method`  VARCHAR(10)     NOT NULL                COMMENT 'GET/POST...',
    `request_params`  TEXT            DEFAULT NULL,
    `response_data`   TEXT            DEFAULT NULL,
    `oper_user_id`    BIGINT UNSIGNED DEFAULT NULL,
    `oper_user_name`  VARCHAR(50)     DEFAULT NULL,
    `oper_ip`         VARCHAR(50)     DEFAULT NULL,
    `user_agent`      VARCHAR(500)    DEFAULT NULL,
    `status`          TINYINT UNSIGNED NOT NULL               COMMENT '0成功/1失败',
    `error_msg`       VARCHAR(2000)   DEFAULT NULL,
    `cost_ms`         INT UNSIGNED    DEFAULT NULL            COMMENT '耗时毫秒',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user`        (`oper_user_id`),
    KEY `idx_created_at`  (`created_at`),
    KEY `idx_status`      (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ============================
-- 商品分类(树形，最多三级)
-- ============================
CREATE TABLE IF NOT EXISTS `biz_category` (
    `id`              BIGINT UNSIGNED NOT NULL,
    `parent_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '父分类，0=根',
    `path`            VARCHAR(500)    NOT NULL                COMMENT '路径如 100/110/111，加速祖先查询',
    `level`           TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '层级 1/2/3',
    `name`            VARCHAR(50)     NOT NULL,
    `icon`            VARCHAR(255)    DEFAULT NULL,
    `description`     VARCHAR(500)    DEFAULT NULL,
    `sort_order`      INT             NOT NULL DEFAULT 0,
    `status`          TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '1启用/0停用',
    `item_count`      INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '在售商品数（冗余）',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_parent`  (`parent_id`),
    KEY `idx_path`    (`path`),
    KEY `idx_status`  (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

-- 种子数据：三级分类示例
INSERT IGNORE INTO `biz_category`(`id`, `parent_id`, `path`, `level`, `name`, `sort_order`) VALUES
-- 一级分类
(100, 0, '100',         1, '艺术品', 1),
(200, 0, '200',         1, '收藏品', 2),
(300, 0, '300',         1, '数码',   3),
(400, 0, '400',         1, '奢侈品', 4),
(500, 0, '500',         1, '珠宝',   5),
(900, 0, '900',         1, '其他',   99),
-- 二级：艺术品
(110, 100, '100/110',   2, '字画',  1),
(120, 100, '100/120',   2, '雕塑',  2),
-- 三级：字画下
(111, 110, '100/110/111', 3, '国画', 1),
(112, 110, '100/110/112', 3, '油画', 2),
(113, 110, '100/110/113', 3, '书法', 3),
-- 二级：数码
(310, 300, '300/310',   2, '手机',  1),
(320, 300, '300/320',   2, '电脑',  2),
(330, 300, '300/330',   2, '相机',  3),
-- 二级：其他
(910, 900, '900/910',   2, '其他',  1);

-- ============================
-- 登录日志
-- ============================
CREATE TABLE IF NOT EXISTS `sys_login_log` (
    `id`              BIGINT UNSIGNED NOT NULL,
    `username`        VARCHAR(50)     NOT NULL,
    `user_id`         BIGINT UNSIGNED DEFAULT NULL,
    `ip`              VARCHAR(50)     DEFAULT NULL,
    `browser`         VARCHAR(50)     DEFAULT NULL,
    `os`              VARCHAR(50)     DEFAULT NULL,
    `status`          TINYINT UNSIGNED NOT NULL               COMMENT '0成功/1失败',
    `msg`             VARCHAR(255)    DEFAULT NULL,
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_username`    (`username`),
    KEY `idx_created_at`  (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';

-- ============================
-- 拍卖商品（核心表）
-- ============================
CREATE TABLE IF NOT EXISTS `biz_auction_item` (
    `id`              BIGINT UNSIGNED NOT NULL,
    `title`           VARCHAR(100)    NOT NULL,
    `subtitle`        VARCHAR(200)    DEFAULT NULL,
    `description`     LONGTEXT                                COMMENT '富文本HTML',
    `category_id`     BIGINT UNSIGNED NOT NULL,
    `category_path`   VARCHAR(500)    NOT NULL                COMMENT '冗余分类路径',
    `cover_image`     VARCHAR(500)    NOT NULL,
    `images`          JSON            DEFAULT NULL            COMMENT '["url1","url2",...]',
    `seller_id`       BIGINT UNSIGNED NOT NULL,

    `auction_type`    TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '1英式/2荷兰式(预留)',
    `start_price`     DECIMAL(12,2)   NOT NULL,
    `current_price`   DECIMAL(12,2)   NOT NULL                COMMENT '当前价(初始=起拍价)',
    `bid_increment`   DECIMAL(12,2)   NOT NULL                COMMENT '加价幅度',
    `buy_now_price`   DECIMAL(12,2)   DEFAULT NULL            COMMENT '一口价',
    `deposit`         DECIMAL(12,2)   NOT NULL DEFAULT 0      COMMENT '保证金',

    `start_time`      DATETIME        NOT NULL,
    `end_time`        DATETIME        NOT NULL,
    `actual_end_time` DATETIME        DEFAULT NULL            COMMENT '实际结束(反狙击延长)',

    `status`          TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '1待审/2待开/3进行/4已结/5已成/6流拍/7下架',
    `audit_status`    TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '0待审/1通过/2驳回',
    `audit_remark`    VARCHAR(255)    DEFAULT NULL,
    `audit_by`        BIGINT UNSIGNED DEFAULT NULL,
    `audit_at`        DATETIME        DEFAULT NULL,

    `winner_id`       BIGINT UNSIGNED DEFAULT NULL,
    `final_price`     DECIMAL(12,2)   DEFAULT NULL,

    `bid_count`       INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '出价次数(冗余)',
    `view_count`      INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '浏览数(冗余)',
    `favorite_count`  INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '收藏数',

    `is_anti_snipe`   TINYINT UNSIGNED NOT NULL DEFAULT 1,
    `anti_snipe_min`  INT UNSIGNED    NOT NULL DEFAULT 5      COMMENT '反狙击延长分钟',

    `tenant_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_by`      BIGINT UNSIGNED NOT NULL,
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`      BIGINT UNSIGNED DEFAULT NULL,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT UNSIGNED NOT NULL DEFAULT 0,
    `version`         INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '乐观锁',

    PRIMARY KEY (`id`),
    KEY `idx_seller`         (`seller_id`),
    KEY `idx_category`       (`category_id`),
    KEY `idx_status_endtime` (`status`, `end_time`),
    KEY `idx_status_created` (`status`, `created_at`),
    KEY `idx_endtime`        (`end_time`),
    KEY `idx_winner`         (`winner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖商品';

-- ============================
-- 出价记录
-- ============================
CREATE TABLE IF NOT EXISTS `biz_bid` (
    `id`                BIGINT UNSIGNED NOT NULL,
    `item_id`           BIGINT UNSIGNED NOT NULL,
    `bidder_id`         BIGINT UNSIGNED NOT NULL,
    `bid_price`         DECIMAL(12,2)   NOT NULL,
    `bid_time`          DATETIME(3)     NOT NULL                COMMENT '毫秒级时间',
    `bid_type`          TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '1正常/2自动/3一口价',
    `status`            TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '1有效/2已被超/3已撤销',
    `client_ip`         VARCHAR(50)     DEFAULT NULL,
    `client_request_id` VARCHAR(64)     DEFAULT NULL            COMMENT '客户端幂等ID',
    `tenant_id`         BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_request_id`    (`client_request_id`),
    KEY `idx_item_time`           (`item_id`, `bid_time` DESC),
    KEY `idx_item_price`          (`item_id`, `bid_price` DESC),
    KEY `idx_bidder`              (`bidder_id`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出价记录';
