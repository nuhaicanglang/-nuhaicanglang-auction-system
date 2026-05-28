-- Day 29 慢 SQL 复核脚本
-- 用法：docker exec -i auction-mysql mysql -uroot -proot123456 auction < test/ops/day29-sql-review.sql
-- 说明：本脚本只执行 EXPLAIN/SHOW，不修改数据结构。

USE auction;

-- 1. 商品列表：状态 + 创建时间倒序，对应 /api/items?status=3&sort=created_at
EXPLAIN SELECT id, title, category_id, seller_id, current_price, status, created_at
FROM biz_auction_item
WHERE deleted = 0 AND status = 3
ORDER BY created_at DESC
LIMIT 10;

-- 2. 商品列表：状态 + 当前价排序，对应 /api/items?status=3&sort=current_price
EXPLAIN SELECT id, title, category_id, seller_id, current_price, status, created_at
FROM biz_auction_item
WHERE deleted = 0 AND status = 3
ORDER BY current_price ASC
LIMIT 10;

-- 3. 商品列表：分类 + 状态 + 创建时间，常见分类页查询
EXPLAIN SELECT id, title, category_id, seller_id, current_price, status, created_at
FROM biz_auction_item
WHERE deleted = 0 AND category_id = 100100 AND status = 3
ORDER BY created_at DESC
LIMIT 10;

-- 4. 商品列表：关键词 LIKE 查询。该查询对大数据量不友好，推荐走 ES 搜索接口。
EXPLAIN SELECT id, title, category_id, seller_id, current_price, status, created_at
FROM biz_auction_item
WHERE deleted = 0 AND status = 3 AND title LIKE '%国画%'
ORDER BY created_at DESC
LIMIT 10;

-- 5. 出价记录：商品详情页出价列表，依赖 idx_item_time
EXPLAIN SELECT id, item_id, bidder_id, bid_price, bid_time, status
FROM biz_bid
WHERE item_id = 313631694601076736
ORDER BY bid_time DESC
LIMIT 20;

-- 6. 出价消费者：查找前一个最高有效出价，依赖 idx_item_price
EXPLAIN SELECT id, item_id, bidder_id, bid_price, bid_time, status
FROM biz_bid
WHERE item_id = 313631694601076736 AND status = 1 AND bidder_id <> 1
ORDER BY bid_price DESC
LIMIT 1;

-- 7. 订单列表：买家视角，依赖 idx_buyer_status_created
EXPLAIN SELECT id, order_no, item_id, buyer_id, seller_id, status, created_at
FROM biz_order
WHERE buyer_id = 1 AND status = 1
ORDER BY created_at DESC
LIMIT 10;

-- 8. 订单列表：卖家视角，依赖 idx_seller_status_created
EXPLAIN SELECT id, order_no, item_id, buyer_id, seller_id, status, created_at
FROM biz_order
WHERE seller_id = 1 AND status = 2
ORDER BY created_at DESC
LIMIT 10;

-- 9. 钱包流水：用户流水分页，依赖 idx_user_created
EXPLAIN SELECT id, transaction_no, user_id, action_type, amount, created_at
FROM biz_wallet_transaction
WHERE user_id = 1
ORDER BY created_at DESC
LIMIT 20;

-- 10. 信用日志：用户信用日志分页，依赖 idx_user_created
EXPLAIN SELECT id, user_id, event_type, delta_score, created_at
FROM biz_credit_log
WHERE user_id = 1
ORDER BY created_at DESC
LIMIT 20;

-- 11. 支付超时扫描/关闭，依赖 idx_pay_deadline
EXPLAIN SELECT id, order_no, status, pay_deadline
FROM biz_order
WHERE status = 1 AND pay_deadline <= NOW()
ORDER BY pay_deadline ASC
LIMIT 50;

-- 12. 当前表索引总览
SHOW INDEX FROM biz_auction_item;
SHOW INDEX FROM biz_bid;
SHOW INDEX FROM biz_order;
SHOW INDEX FROM biz_wallet_transaction;
SHOW INDEX FROM biz_credit_log;
