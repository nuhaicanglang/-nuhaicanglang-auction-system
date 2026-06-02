USE auction;

-- 标准演示数据初始化脚本。
-- 用途：
-- 1. 首次创建 MySQL 容器时，由 docker-entrypoint 自动执行。
-- 2. 已有开发库需要清理测试数据时，可手动执行：
--    docker exec -i auction-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 auction < deploy/mysql/demo-data.sql

SET FOREIGN_KEY_CHECKS = 0;

-- 清理测试痕迹和业务演示数据。保留角色、权限、分类等系统基础字典。
DELETE FROM `biz_review`;
DELETE FROM `biz_payment`;
DELETE FROM `biz_order`;
DELETE FROM `biz_bid`;
DELETE FROM `biz_favorite`;
DELETE FROM `biz_notification`;
DELETE FROM `biz_wallet_transaction`;
DELETE FROM `biz_wallet`;
DELETE FROM `biz_credit_log`;
DELETE FROM `biz_credit`;
DELETE FROM `biz_auction_item`;
DELETE FROM `sys_oper_log`;
DELETE FROM `sys_login_log`;
DELETE FROM `sys_user_role`;
DELETE FROM `sys_user`;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `biz_category`(`id`, `parent_id`, `path`, `level`, `name`, `icon`, `description`, `sort_order`, `status`, `item_count`, `created_at`, `updated_at`, `deleted`)
VALUES
(100, 0, '100', 1, '艺术品', NULL, '书画、雕塑等艺术收藏类拍品', 1, 1, 0, NOW(), NOW(), 0),
(110, 100, '100/110', 2, '字画', NULL, '国画、油画、书法', 1, 1, 0, NOW(), NOW(), 0),
(111, 110, '100/110/111', 3, '国画', NULL, '山水、花鸟、人物国画作品', 1, 1, 0, NOW(), NOW(), 0),
(112, 110, '100/110/112', 3, '油画', NULL, '现代与古典油画作品', 2, 1, 0, NOW(), NOW(), 0),
(113, 110, '100/110/113', 3, '书法', NULL, '行书、楷书、草书等作品', 3, 1, 0, NOW(), NOW(), 0),
(120, 100, '100/120', 2, '雕塑', NULL, '铜雕、石雕与当代雕塑', 2, 1, 0, NOW(), NOW(), 0),
(200, 0, '200', 1, '收藏品', NULL, '生活方式与怀旧收藏类拍品', 2, 1, 0, NOW(), NOW(), 0),
(300, 0, '300', 1, '数码', NULL, '手机、电脑、相机等数码设备', 3, 1, 0, NOW(), NOW(), 0),
(310, 300, '300/310', 2, '手机', NULL, '手机与移动终端', 1, 1, 0, NOW(), NOW(), 0),
(320, 300, '300/320', 2, '电脑', NULL, '笔记本、工作站等设备', 2, 1, 0, NOW(), NOW(), 0),
(330, 300, '300/330', 2, '相机', NULL, '胶片与数码影像设备', 3, 1, 0, NOW(), NOW(), 0),
(400, 0, '400', 1, '奢侈品', NULL, '腕表与高端生活方式单品', 4, 1, 0, NOW(), NOW(), 0),
(500, 0, '500', 1, '珠宝', NULL, '玉器、手镯和珠宝饰品', 5, 1, 0, NOW(), NOW(), 0),
(900, 0, '900', 1, '其他', NULL, '补充类目', 99, 1, 0, NOW(), NOW(), 0),
(910, 900, '900/910', 2, '文创杂项', NULL, '礼盒、文创与其他藏品', 1, 1, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
`parent_id` = VALUES(`parent_id`),
`path` = VALUES(`path`),
`level` = VALUES(`level`),
`name` = VALUES(`name`),
`icon` = VALUES(`icon`),
`description` = VALUES(`description`),
`sort_order` = VALUES(`sort_order`),
`status` = VALUES(`status`),
`item_count` = 0,
`updated_at` = NOW(),
`deleted` = 0;

-- 所有演示账号统一密码：123456
SET @demo_password = '$2a$10$BqMXaJLPIQAw6TODtx46zuwdBoBPAbVEIWV22xDldtEjGvOolcBLK';

INSERT INTO `sys_user`(
    `id`, `username`, `nickname`, `password`, `email`, `phone`, `avatar`, `gender`,
    `real_name`, `status`, `blacklist_reason`, `blacklisted_by`, `blacklisted_at`,
    `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`
) VALUES
(1, 'admin', '系统超级管理员', @demo_password, 'admin@auction.local', '18800000001', NULL, 0, '系统管理员', 1, NULL, NULL, NULL, 0, NULL, NOW(), NULL, NOW(), 0),
(2, 'ops_admin', '运营管理员', @demo_password, 'ops@auction.local', '18800000002', NULL, 0, '运营管理员', 1, NULL, NULL, NULL, 0, 1, NOW(), 1, NOW(), 0),
(1001, 'seller_art', '文华雅集艺术馆', @demo_password, 'seller.art@auction.local', '18800001001', NULL, 0, '陈文华', 1, NULL, NULL, NULL, 0, 1, NOW(), 1, NOW(), 0),
(1002, 'seller_digital', '极光数码严选', @demo_password, 'seller.digital@auction.local', '18800001002', NULL, 0, '林启明', 1, NULL, NULL, NULL, 0, 1, NOW(), 1, NOW(), 0),
(1003, 'seller_luxury', '臻品生活馆', @demo_password, 'seller.luxury@auction.local', '18800001003', NULL, 0, '周澜', 1, NULL, NULL, NULL, 0, 1, NOW(), 1, NOW(), 0),
(2001, 'buyer_vip', '企业采购买家', @demo_password, 'buyer.vip@auction.local', '18800002001', NULL, 0, '王澄', 1, NULL, NULL, NULL, 0, 1, NOW(), 1, NOW(), 0),
(2002, 'buyer_standard', '个人收藏买家', @demo_password, 'buyer.standard@auction.local', '18800002002', NULL, 0, '李安然', 1, NULL, NULL, NULL, 0, 1, NOW(), 1, NOW(), 0),
(2003, 'buyer_risk', '信用观察用户', @demo_password, 'buyer.risk@auction.local', '18800002003', NULL, 0, '赵谨', 1, NULL, NULL, NULL, 0, 1, NOW(), 1, NOW(), 0),
(3001, 'blacklisted_user', '黑名单演示用户', @demo_password, 'blacklisted@auction.local', '18800003001', NULL, 0, '钱某', 2, '演示账号：异常竞价风控命中', 1, NOW(), 0, 1, NOW(), 1, NOW(), 0);

INSERT INTO `sys_user_role`(`user_id`, `role_id`) VALUES
(1, 1),
(2, 2),
(1001, 3),
(1002, 3),
(1003, 3),
(2001, 3),
(2002, 3),
(2003, 3),
(3001, 3);

INSERT INTO `biz_wallet`(`id`, `user_id`, `balance`, `frozen_balance`, `status`, `tenant_id`, `created_at`, `updated_at`, `version`) VALUES
(9001, 1,    100000.00, 0.00, 1, 0, NOW(), NOW(), 0),
(9002, 2,     50000.00, 0.00, 1, 0, NOW(), NOW(), 0),
(9003, 1001,   5000.00, 0.00, 1, 0, NOW(), NOW(), 0),
(9004, 1002,   5000.00, 0.00, 1, 0, NOW(), NOW(), 0),
(9005, 1003,   5000.00, 0.00, 1, 0, NOW(), NOW(), 0),
(9006, 2001,  25000.00, 0.00, 1, 0, NOW(), NOW(), 0),
(9007, 2002,  12000.00, 0.00, 1, 0, NOW(), NOW(), 0),
(9008, 2003,   1500.00, 0.00, 1, 0, NOW(), NOW(), 0),
(9009, 3001,      0.00, 0.00, 0, 0, NOW(), NOW(), 0);

INSERT INTO `biz_wallet_transaction`(
    `id`, `transaction_no`, `wallet_id`, `user_id`, `action_type`, `direction`, `amount`,
    `balance_before`, `balance_after`, `frozen_before`, `frozen_after`, `biz_type`,
    `biz_id`, `related_item_id`, `operator_id`, `remark`, `idempotent_key`, `tenant_id`, `created_at`
) VALUES
(910001, 'TXN-DEMO-ADMIN-INIT', 9001, 1, 'RECHARGE', 1, 100000.00, 0.00, 100000.00, 0.00, 0.00, 'INIT', 'DEMO', NULL, 1, '初始化超级管理员资金', 'demo-init-wallet-admin', 0, NOW()),
(910002, 'TXN-DEMO-OPS-INIT', 9002, 2, 'RECHARGE', 1, 50000.00, 0.00, 50000.00, 0.00, 0.00, 'INIT', 'DEMO', NULL, 1, '初始化运营管理员资金', 'demo-init-wallet-ops', 0, NOW()),
(910003, 'TXN-DEMO-ART-INIT', 9003, 1001, 'RECHARGE', 1, 5000.00, 0.00, 5000.00, 0.00, 0.00, 'INIT', 'DEMO', NULL, 1, '初始化卖家保证金账户', 'demo-init-wallet-seller-art', 0, NOW()),
(910004, 'TXN-DEMO-DIGITAL-INIT', 9004, 1002, 'RECHARGE', 1, 5000.00, 0.00, 5000.00, 0.00, 0.00, 'INIT', 'DEMO', NULL, 1, '初始化卖家保证金账户', 'demo-init-wallet-seller-digital', 0, NOW()),
(910005, 'TXN-DEMO-LUXURY-INIT', 9005, 1003, 'RECHARGE', 1, 5000.00, 0.00, 5000.00, 0.00, 0.00, 'INIT', 'DEMO', NULL, 1, '初始化卖家保证金账户', 'demo-init-wallet-seller-luxury', 0, NOW()),
(910006, 'TXN-DEMO-BUYER-VIP-INIT', 9006, 2001, 'RECHARGE', 1, 25000.00, 0.00, 25000.00, 0.00, 0.00, 'INIT', 'DEMO', NULL, 1, '初始化企业买家竞拍资金', 'demo-init-wallet-buyer-vip', 0, NOW()),
(910007, 'TXN-DEMO-BUYER-STD-INIT', 9007, 2002, 'RECHARGE', 1, 12000.00, 0.00, 12000.00, 0.00, 0.00, 'INIT', 'DEMO', NULL, 1, '初始化个人买家竞拍资金', 'demo-init-wallet-buyer-standard', 0, NOW()),
(910008, 'TXN-DEMO-BUYER-RISK-INIT', 9008, 2003, 'RECHARGE', 1, 1500.00, 0.00, 1500.00, 0.00, 0.00, 'INIT', 'DEMO', NULL, 1, '初始化信用观察用户资金', 'demo-init-wallet-buyer-risk', 0, NOW());

INSERT INTO `biz_credit`(`id`, `user_id`, `score`, `level_name`, `status`, `last_event_at`, `tenant_id`, `created_at`, `updated_at`) VALUES
(9201, 1, 100, '卓越', 1, NOW(), 0, NOW(), NOW()),
(9202, 2, 96, '卓越', 1, NOW(), 0, NOW(), NOW()),
(9203, 1001, 92, '优秀', 1, NOW(), 0, NOW(), NOW()),
(9204, 1002, 90, '优秀', 1, NOW(), 0, NOW(), NOW()),
(9205, 1003, 93, '优秀', 1, NOW(), 0, NOW(), NOW()),
(9206, 2001, 95, '卓越', 1, NOW(), 0, NOW(), NOW()),
(9207, 2002, 82, '良好', 1, NOW(), 0, NOW(), NOW()),
(9208, 2003, 55, '观察', 1, NOW(), 0, NOW(), NOW()),
(9209, 3001, 20, '禁用', 0, NOW(), 0, NOW(), NOW());

INSERT INTO `biz_credit_log`(
    `id`, `user_id`, `event_type`, `related_id`, `delta_score`, `score_before`,
    `score_after`, `remark`, `idempotent_key`, `tenant_id`, `created_at`
) VALUES
(9301, 2001, 'INIT', 'DEMO', 15, 80, 95, '企业买家历史履约良好', 'demo-credit-buyer-vip', 0, NOW()),
(9302, 2002, 'INIT', 'DEMO', 2, 80, 82, '个人买家标准信用初始化', 'demo-credit-buyer-standard', 0, NOW()),
(9303, 2003, 'RISK_WARN', 'DEMO', -25, 80, 55, '信用观察演示：多次取消订单', 'demo-credit-buyer-risk', 0, NOW()),
(9304, 3001, 'BLACKLIST', 'DEMO', -60, 80, 20, '黑名单演示：异常竞价风控命中', 'demo-credit-blacklisted', 0, NOW());

INSERT INTO `biz_auction_item`(
    `id`, `title`, `subtitle`, `description`, `category_id`, `category_path`, `cover_image`, `images`, `seller_id`,
    `auction_type`, `start_price`, `current_price`, `bid_increment`, `buy_now_price`, `deposit`,
    `start_time`, `end_time`, `actual_end_time`, `status`, `audit_status`, `audit_remark`, `audit_by`, `audit_at`,
    `winner_id`, `final_price`, `bid_count`, `view_count`, `favorite_count`, `is_anti_snipe`, `anti_snipe_min`,
    `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`, `version`
) VALUES
(100001, '溪山清远设色山水长卷', '艺术馆认证装裱，适合书房与会客厅陈列', '<p>纸本设色山水长卷，构图层次清晰，附艺术馆入库记录。</p>', 111, '100/110/111', '/sample-items/art-landscape.svg', JSON_ARRAY('/sample-items/art-landscape.svg'), 1001, 1, 680.00, 860.00, 30.00, 1280.00, 60.00, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 18 HOUR), NULL, 3, 1, '标准演示数据：审核通过', 1, NOW(), NULL, NULL, 2, 168, 2, 1, 5, 0, 1001, DATE_SUB(NOW(), INTERVAL 3 DAY), 1, NOW(), 0, 0),
(100002, '墨韵行书四尺条幅', '名家风格行书，装裱完成可直接悬挂', '<p>行书条幅笔势连贯，适合作为办公室、茶室和书房陈设。</p>', 113, '100/110/113', '/sample-items/art-calligraphy.svg', JSON_ARRAY('/sample-items/art-calligraphy.svg'), 1001, 1, 520.00, 520.00, 20.00, 760.00, 50.00, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY), NULL, 3, 1, '标准演示数据：审核通过', 1, NOW(), NULL, NULL, 0, 96, 1, 1, 5, 0, 1001, DATE_SUB(NOW(), INTERVAL 3 DAY), 1, NOW(), 0, 0),
(100003, '铜鎏金人物桌面雕塑', '铜质鎏金工艺，适合陈列收藏', '<p>桌面雕塑细节完整，适合客厅边柜、展厅和收藏空间。</p>', 120, '100/120', '/sample-items/sculpture-bronze.svg', JSON_ARRAY('/sample-items/sculpture-bronze.svg'), 1001, 1, 1280.00, 1350.00, 50.00, 1880.00, 120.00, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL 2 DAY), NULL, 3, 1, '标准演示数据：审核通过', 1, NOW(), NULL, NULL, 1, 142, 1, 1, 5, 0, 1001, DATE_SUB(NOW(), INTERVAL 4 DAY), 1, NOW(), 0, 0),
(100004, '经典旁轴胶片相机套装', '带原装皮套，镜头通透', '<p>收藏向胶片相机，机身成色优秀，附带皮套与肩带。</p>', 330, '300/330', '/sample-items/camera-vintage.svg', JSON_ARRAY('/sample-items/camera-vintage.svg'), 1002, 1, 1680.00, 1800.00, 60.00, 2360.00, 150.00, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 20 HOUR), NULL, 3, 1, '标准演示数据：审核通过', 1, NOW(), NULL, NULL, 2, 210, 1, 1, 5, 0, 1002, DATE_SUB(NOW(), INTERVAL 2 DAY), 1, NOW(), 0, 0),
(100005, 'Aurora X1 旗舰手机', '12GB + 512GB，成色近新', '<p>高端旗舰手机，屏幕与机身状态良好，适合数码爱好者竞拍。</p>', 310, '300/310', '/sample-items/smartphone-aurora.svg', JSON_ARRAY('/sample-items/smartphone-aurora.svg'), 1002, 1, 2399.00, 2399.00, 80.00, 3299.00, 200.00, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 3 DAY), NULL, 3, 1, '标准演示数据：审核通过', 1, NOW(), NULL, NULL, 0, 188, 2, 1, 5, 0, 1002, DATE_SUB(NOW(), INTERVAL 2 DAY), 1, NOW(), 0, 0),
(100006, 'Carbon Pro 商务轻薄本', '14英寸商务超轻薄，附原装充电器', '<p>轻薄本适合办公与差旅使用，外观保持良好，已完成基础检测。</p>', 320, '300/320', '/sample-items/laptop-carbon.svg', JSON_ARRAY('/sample-items/laptop-carbon.svg'), 1002, 1, 3299.00, 3299.00, 100.00, 4399.00, 300.00, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 4 DAY), NULL, 3, 1, '标准演示数据：审核通过', 1, NOW(), NULL, NULL, 0, 160, 1, 1, 5, 0, 1002, DATE_SUB(NOW(), INTERVAL 1 DAY), 1, NOW(), 0, 0),
(100007, 'Chronos 蓝钢指针机械腕表', '背透机芯，日常佩戴与收藏皆宜', '<p>机械腕表表盘干净，走时稳定，适合作为精品腕表竞拍。</p>', 400, '400', '/sample-items/watch-chrono.svg', JSON_ARRAY('/sample-items/watch-chrono.svg'), 1003, 1, 2860.00, 2860.00, 120.00, 3980.00, 300.00, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 2 DAY), NULL, 3, 1, '标准演示数据：审核通过', 1, NOW(), NULL, NULL, 0, 235, 1, 1, 5, 0, 1003, DATE_SUB(NOW(), INTERVAL 4 DAY), 1, NOW(), 0, 0),
(100008, '和田玉圆条手镯', '玉质温润，附检测证书', '<p>手镯玉质温润，证书信息齐全，适合珠宝拍卖场景展示。</p>', 500, '500', '/sample-items/jade-bracelet.svg', JSON_ARRAY('/sample-items/jade-bracelet.svg'), 1003, 1, 1880.00, 1960.00, 80.00, 2680.00, 180.00, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY), NULL, 3, 1, '标准演示数据：审核通过', 1, NOW(), NULL, NULL, 1, 172, 2, 1, 5, 0, 1003, DATE_SUB(NOW(), INTERVAL 3 DAY), 1, NOW(), 0, 0),
(100009, '胡桃木黑胶唱片机套装', '含试听唱片，复古生活方式拍品', '<p>复古黑胶唱机，胡桃木纹机身，适合家居搭配与音乐收藏。</p>', 200, '200', '/sample-items/vinyl-record.svg', JSON_ARRAY('/sample-items/vinyl-record.svg'), 1003, 1, 980.00, 980.00, 40.00, 1480.00, 80.00, DATE_ADD(NOW(), INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL 3 DAY), NULL, 2, 1, '标准演示数据：审核通过，待开拍', 1, NOW(), NULL, NULL, 0, 118, 0, 1, 5, 0, 1003, DATE_SUB(NOW(), INTERVAL 1 DAY), 1, NOW(), 0, 0),
(100010, '限量树脂钢笔礼盒', '商务礼赠向，礼盒齐全', '<p>树脂笔杆钢笔礼盒，适合作为文房与收藏类商品补充。</p>', 910, '900/910', '/sample-items/fountain-pen.svg', JSON_ARRAY('/sample-items/fountain-pen.svg'), 1001, 1, 460.00, 460.00, 20.00, 720.00, 30.00, DATE_ADD(NOW(), INTERVAL 8 HOUR), DATE_ADD(NOW(), INTERVAL 5 DAY), NULL, 2, 1, '标准演示数据：审核通过，待开拍', 1, NOW(), NULL, NULL, 0, 78, 0, 1, 5, 0, 1001, DATE_SUB(NOW(), INTERVAL 1 DAY), 1, NOW(), 0, 0),
(100011, '未审核拍品：岭南花鸟册页', '用于管理员审核流程演示', '<p>待审核艺术品，可在后台商品审核中通过或驳回。</p>', 111, '100/110/111', '/sample-items/art-landscape.svg', JSON_ARRAY('/sample-items/art-landscape.svg'), 1001, 1, 880.00, 880.00, 40.00, 1280.00, 80.00, DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 4 DAY), NULL, 1, 0, NULL, NULL, NULL, NULL, NULL, 0, 12, 0, 1, 5, 0, 1001, NOW(), NULL, NOW(), 0, 0),
(100012, '未审核拍品：便携摄影灯套装', '用于管理员审核流程演示', '<p>待审核数码配件，可在后台商品审核中完成审核。</p>', 330, '300/330', '/sample-items/camera-vintage.svg', JSON_ARRAY('/sample-items/camera-vintage.svg'), 1002, 1, 360.00, 360.00, 20.00, 520.00, 30.00, DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 4 DAY), NULL, 1, 0, NULL, NULL, NULL, NULL, NULL, 0, 9, 0, 1, 5, 0, 1002, NOW(), NULL, NOW(), 0, 0),
(100013, '已成交演示：青玉手镯专场', '用于买家订单、卖家订单和评价流程演示', '<p>该拍品已成交，关联一笔已完成订单。</p>', 500, '500', '/sample-items/jade-bracelet.svg', JSON_ARRAY('/sample-items/jade-bracelet.svg'), 1003, 1, 2680.00, 3580.00, 100.00, 4200.00, 200.00, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 5, 1, '标准演示数据：成交订单', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), 2001, 3580.00, 3, 420, 0, 1, 5, 0, 1003, DATE_SUB(NOW(), INTERVAL 6 DAY), 1, NOW(), 0, 0),
(100014, '流拍演示：复古唱片试听机', '用于流拍状态展示', '<p>该拍品无人达到保留价，作为流拍状态演示。</p>', 200, '200', '/sample-items/vinyl-record.svg', JSON_ARRAY('/sample-items/vinyl-record.svg'), 1003, 1, 1880.00, 1880.00, 80.00, 2680.00, 160.00, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 6, 1, '标准演示数据：流拍', 1, DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, NULL, 0, 160, 0, 1, 5, 0, 1003, DATE_SUB(NOW(), INTERVAL 5 DAY), 1, NOW(), 0, 0);

INSERT INTO `biz_bid`(
    `id`, `item_id`, `bidder_id`, `bid_price`, `bid_time`, `bid_type`, `status`,
    `client_ip`, `client_request_id`, `tenant_id`, `created_at`
) VALUES
(110001, 100001, 2002, 710.00, DATE_SUB(NOW(3), INTERVAL 90 MINUTE), 1, 2, '127.0.0.1', 'demo-bid-100001-1', 0, NOW()),
(110002, 100001, 2001, 860.00, DATE_SUB(NOW(3), INTERVAL 30 MINUTE), 1, 1, '127.0.0.1', 'demo-bid-100001-2', 0, NOW()),
(110003, 100003, 2001, 1350.00, DATE_SUB(NOW(3), INTERVAL 20 MINUTE), 1, 1, '127.0.0.1', 'demo-bid-100003-1', 0, NOW()),
(110004, 100004, 2002, 1800.00, DATE_SUB(NOW(3), INTERVAL 40 MINUTE), 1, 1, '127.0.0.1', 'demo-bid-100004-1', 0, NOW()),
(110005, 100008, 2002, 1960.00, DATE_SUB(NOW(3), INTERVAL 70 MINUTE), 1, 1, '127.0.0.1', 'demo-bid-100008-1', 0, NOW()),
(110006, 100013, 2002, 2880.00, DATE_SUB(NOW(3), INTERVAL 4 DAY), 1, 2, '127.0.0.1', 'demo-bid-100013-1', 0, NOW()),
(110007, 100013, 2001, 3280.00, DATE_SUB(NOW(3), INTERVAL 3 DAY), 1, 2, '127.0.0.1', 'demo-bid-100013-2', 0, NOW()),
(110008, 100013, 2001, 3580.00, DATE_SUB(NOW(3), INTERVAL 2 DAY), 1, 1, '127.0.0.1', 'demo-bid-100013-3', 0, NOW());

INSERT INTO `biz_order`(
    `id`, `order_no`, `item_id`, `item_title`, `item_cover_image`, `buyer_id`, `seller_id`, `bid_id`,
    `deal_price`, `deposit_amount`, `pay_amount`, `status`, `pay_deadline`, `paid_at`, `shipped_at`,
    `completed_at`, `closed_at`, `close_reason`, `tenant_id`, `created_at`, `updated_at`
) VALUES
(120001, 'ORD-DEMO-202606020001', 100013, '已成交演示：青玉手镯专场', '/sample-items/jade-bracelet.svg', 2001, 1003, 110008, 3580.00, 200.00, 3380.00, 4, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 36 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), NULL, NULL, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW());

INSERT INTO `biz_payment`(
    `id`, `payment_no`, `order_id`, `order_no`, `payer_id`, `amount`, `pay_method`, `status`,
    `paid_at`, `idempotent_key`, `remark`, `tenant_id`, `created_at`, `updated_at`
) VALUES
(130001, 'PAY-DEMO-202606020001', 120001, 'ORD-DEMO-202606020001', 2001, 3380.00, 'WALLET', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), 'demo-payment-120001', '演示订单钱包支付', 0, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW());

INSERT INTO `biz_review`(
    `id`, `order_id`, `item_id`, `reviewer_id`, `reviewee_id`, `role_type`, `score`, `content`,
    `status`, `tenant_id`, `created_at`, `updated_at`
) VALUES
(140001, 120001, 100013, 2001, 1003, 'BUYER', 5, '证书齐全、发货及时，适合作为企业演示的已完成订单。', 1, 0, DATE_SUB(NOW(), INTERVAL 20 HOUR), NOW()),
(140002, 120001, 100013, 1003, 2001, 'SELLER', 5, '买家付款迅速，沟通顺畅。', 1, 0, DATE_SUB(NOW(), INTERVAL 19 HOUR), NOW());

INSERT INTO `biz_favorite`(`id`, `user_id`, `item_id`, `tenant_id`, `created_at`) VALUES
(150001, 2001, 100001, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(150002, 2001, 100004, 0, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(150003, 2002, 100005, 0, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(150004, 2002, 100007, 0, DATE_SUB(NOW(), INTERVAL 4 HOUR));

INSERT INTO `biz_notification`(
    `id`, `user_id`, `type`, `title`, `content`, `related_item_id`, `is_read`, `read_at`, `created_at`
) VALUES
(160001, 2001, 2, '竞拍领先提醒', '你在“溪山清远设色山水长卷”的出价暂时领先，请关注倒计时。', 100001, 0, NULL, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(160002, 2002, 1, '出价被超越', '你在“溪山清远设色山水长卷”的出价已被超越，可前往详情页继续竞价。', 100001, 0, NULL, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(160003, 2001, 2, '订单已完成', '“已成交演示：青玉手镯专场”订单已完成，欢迎进行评价。', 100013, 1, DATE_SUB(NOW(), INTERVAL 18 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR));

UPDATE `biz_category` c
SET c.`item_count` = (
    SELECT COUNT(*)
    FROM `biz_auction_item` i
    WHERE i.`deleted` = 0
      AND i.`status` IN (2, 3)
      AND i.`category_path` LIKE CONCAT(c.`path`, '%')
);
