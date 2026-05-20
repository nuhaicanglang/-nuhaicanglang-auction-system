if redis.call('SETNX', KEYS[3], ARGV[4]) == 0 then
    return -1
end
redis.call('EXPIRE', KEYS[3], 300)

local status = redis.call('GET', KEYS[4])
if status == ARGV[6] then
    redis.call('DEL', KEYS[3])
    return 0
end

local current = tonumber(redis.call('GET', KEYS[1]) or '0')
local buyNowPrice = tonumber(ARGV[2])

if current > buyNowPrice then
    redis.call('DEL', KEYS[3])
    return 0
end

redis.call('SET', KEYS[1], buyNowPrice)
redis.call('SET', KEYS[4], ARGV[6])

local payload = ARGV[4]..'|'..ARGV[1]..'|'..buyNowPrice..'|'..ARGV[3]..'|'..ARGV[5]
redis.call('LPUSH', KEYS[2], payload)
redis.call('LTRIM', KEYS[2], 0, 999)

return 1
