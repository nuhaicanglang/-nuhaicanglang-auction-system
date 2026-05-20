-- ============================================
-- Day 15 压测准备：批量创建 100 个竞价账号、重置商品
-- 密码与 bidder01 一致（BCrypt hash 相同）
-- ============================================

-- 1. 获取 bidder01 的密码哈希（用于后续 INSERT）
SET @pwd_hash = (SELECT password FROM sys_user WHERE username = 'bidder01' LIMIT 1);

-- 2. 批量创建 bidder02 ~ bidder100（忽略已存在）
-- 使用简单递增雪花ID（基于 bidder01 的 ID + 偏移）
SET @base_id = (SELECT id FROM sys_user WHERE username = 'bidder01' LIMIT 1);
SET @role_id = 3;  -- USER 角色

DROP PROCEDURE IF EXISTS create_bidders;
DELIMITER $$
CREATE PROCEDURE create_bidders()
BEGIN
    DECLARE i INT DEFAULT 2;
    WHILE i <= 100 DO
        INSERT IGNORE INTO sys_user(id, username, nickname, password, status, created_at)
        VALUES (@base_id + i, CONCAT('bidder', LPAD(i, 2, '0')), CONCAT('压测用户', i), @pwd_hash, 1, NOW());
        INSERT IGNORE INTO sys_user_role(user_id, role_id)
        VALUES (@base_id + i, @role_id);
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

CALL create_bidders();
DROP PROCEDURE IF EXISTS create_bidders;

-- 3. 重置测试商品：延长结束时间、清除出价记录、重置价格
UPDATE biz_auction_item
SET status = 3,
    current_price = start_price,
    bid_count = 0,
    winner_id = NULL,
    final_price = NULL,
    actual_end_time = NULL,
    end_time = DATE_ADD(NOW(), INTERVAL 2 HOUR)
WHERE id = 313631694601076736;

-- 清除该商品的所有出价记录
DELETE FROM biz_bid WHERE item_id = 313631694601076736;

-- 4. 验证
SELECT COUNT(*) AS bidder_count FROM sys_user WHERE username LIKE 'bidder%';
SELECT id, username, status FROM sys_user WHERE username LIKE 'bidder%' ORDER BY username LIMIT 5;
SELECT id, status, current_price, start_price, bid_increment, end_time FROM biz_auction_item WHERE id = 313631694601076736;
