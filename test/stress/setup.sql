-- ============================================
-- Day 15 / Day 28 压测准备：批量创建 100 个竞价账号、重置一个拍卖中商品
-- 密码统一复用演示账号 buyer_standard 的 BCrypt hash，对应明文密码：123456
-- ============================================

-- 1. 为 bidder01 准备基础账号（若不存在则自动创建）
SET @pwd_hash = (SELECT password FROM sys_user WHERE username = 'buyer_standard' LIMIT 1);
SET @role_id = 3;  -- USER 角色
SET @base_id = 900001;

INSERT IGNORE INTO sys_user(id, username, nickname, password, status, created_at)
VALUES (@base_id, 'bidder01', '压测用户1', @pwd_hash, 1, NOW());

INSERT IGNORE INTO sys_user_role(user_id, role_id)
VALUES (@base_id, @role_id);

-- 2. 批量创建 bidder02 ~ bidder100（忽略已存在）
-- 使用固定连续 ID，保证脚本可以重复执行
SET @pwd_hash = (SELECT password FROM sys_user WHERE username = 'bidder01' LIMIT 1);

DROP PROCEDURE IF EXISTS create_bidders;
DELIMITER $$
CREATE PROCEDURE create_bidders()
BEGIN
    DECLARE i INT DEFAULT 2;
    WHILE i <= 100 DO
        INSERT IGNORE INTO sys_user(id, username, nickname, password, status, created_at)
        VALUES (@base_id + i - 1, CONCAT('bidder', LPAD(i, 2, '0')), CONCAT('压测用户', i), @pwd_hash, 1, NOW());
        INSERT IGNORE INTO sys_user_role(user_id, role_id)
        VALUES (@base_id + i - 1, @role_id);
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

CALL create_bidders();
DROP PROCEDURE IF EXISTS create_bidders;

-- 补一条显式兜底，避免极端情况下 bidder100 未生成
INSERT IGNORE INTO sys_user(id, username, nickname, password, status, created_at)
VALUES (900100, 'bidder100', '压测用户100', @pwd_hash, 1, NOW());

INSERT IGNORE INTO sys_user_role(user_id, role_id)
VALUES (900100, @role_id);

-- 3. 选择一个拍卖中商品作为压测目标，并重置价格/状态
SET @target_item_id = (
    SELECT id
    FROM biz_auction_item
    WHERE status = 3
    ORDER BY id
    LIMIT 1
);

UPDATE biz_auction_item
SET status = 3,
    current_price = start_price,
    bid_count = 0,
    winner_id = NULL,
    final_price = NULL,
    actual_end_time = NULL,
    end_time = DATE_ADD(NOW(), INTERVAL 2 HOUR)
WHERE id = @target_item_id;

-- 清除该商品的所有出价记录
DELETE FROM biz_bid WHERE item_id = @target_item_id;

-- 4. 验证
SELECT COUNT(*) AS bidder_count FROM sys_user WHERE username LIKE 'bidder%';
SELECT id, username, status FROM sys_user WHERE username LIKE 'bidder%' ORDER BY username LIMIT 5;
SELECT id AS target_item_id, status, current_price, start_price, bid_increment, end_time
FROM biz_auction_item
WHERE id = @target_item_id;
