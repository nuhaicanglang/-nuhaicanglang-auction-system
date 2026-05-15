-- =============================================
-- 出价原子脚本（Redis 端执行，保证并发安全）
-- KEYS[1] = auction:price:<itemId>      当前价
-- KEYS[2] = auction:bid:queue:<itemId>  出价队列
-- KEYS[3] = auction:idem:<requestId>    幂等锁
-- ARGV[1] = userId
-- ARGV[2] = bidPrice      出价金额
-- ARGV[3] = bidTimeMs     出价时间戳（毫秒）
-- ARGV[4] = increment     最小加价幅度
-- ARGV[5] = bidId         出价记录ID（雪花ID）
-- ARGV[6] = requestId     客户端幂等ID
-- 返回: 1=成功, 0=出价不足, -1=重复请求
-- =============================================

-- 1. 幂等检查：SETNX 返回 0 表示 key 已存在 → 重复请求
if redis.call('SETNX', KEYS[3], ARGV[5]) == 0 then
    return -1
end
redis.call('EXPIRE', KEYS[3], 300)

-- 2. 读取当前价，比较出价是否满足 最低加价
local current = tonumber(redis.call('GET', KEYS[1]) or '0')
local newPrice = tonumber(ARGV[2])
local increment = tonumber(ARGV[4])

if newPrice < current + increment then
    -- 出价不足，删除幂等锁（允许重试）
    redis.call('DEL', KEYS[3])
    return 0
end

-- 3. 更新当前价
redis.call('SET', KEYS[1], newPrice)

-- 4. 出价记录入队（后续由消费者持久化到 MySQL）
-- 格式: bidId|userId|bidPrice|bidTimeMs|requestId
local payload = ARGV[5]..'|'..ARGV[1]..'|'..newPrice..'|'..ARGV[3]..'|'..ARGV[6]
redis.call('LPUSH', KEYS[2], payload)
redis.call('LTRIM', KEYS[2], 0, 999)

return 1
