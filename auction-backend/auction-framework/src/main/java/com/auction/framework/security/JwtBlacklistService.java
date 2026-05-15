package com.auction.framework.security;

import com.auction.common.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * JWT 黑名单服务。
 * 用户登出时将当前 token 加入黑名单（Redis），过期时间 = token 剩余有效期。
 * 认证过滤器在解析 token 后先检查黑名单，命中则视为未登录。
 */
@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 将 token 加入黑名单。
     *
     * @param token          完整的 JWT token 字符串
     * @param remainingMs    token 剩余有效毫秒数
     */
    public void addToBlacklist(String token, long remainingMs) {
        if (remainingMs <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(
                RedisKeyConstants.JWT_BLACKLIST_PREFIX + token,
                "1",
                remainingMs,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 判断 token 是否在黑名单中。
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(RedisKeyConstants.JWT_BLACKLIST_PREFIX + token));
    }
}
