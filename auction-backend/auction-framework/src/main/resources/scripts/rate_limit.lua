-- 滑动窗口限流 Lua 脚本
-- KEYS[1] = 限流 key
-- ARGV[1] = 最大请求数
-- ARGV[2] = 窗口时间(秒)
-- 返回 1 表示允许，0 表示拒绝

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local current = tonumber(redis.call('GET', key) or "0")

if current < limit then
    redis.call('INCR', key)
    if current == 0 then
        redis.call('EXPIRE', key, window)
    end
    return 1
else
    return 0
end
