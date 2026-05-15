package com.auction.framework.security;

import com.auction.common.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录失败计数与账号锁定服务。
 * <ul>
 *   <li>连续失败 >= 3 次：前端必须携带图形验证码</li>
 *   <li>连续失败 >= 5 次：锁定账号 15 分钟</li>
 *   <li>登录成功后清零</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class LoginFailService {

    private final StringRedisTemplate redisTemplate;

    /** 需要图形验证码的失败次数阈值 */
    public static final int CAPTCHA_THRESHOLD = 3;

    /** 锁定账号的失败次数阈值 */
    public static final int LOCK_THRESHOLD = 5;

    /** 锁定时长（分钟） */
    private static final long LOCK_MINUTES = 15;

    /** 失败计数过期时间（分钟），与锁定时长一致 */
    private static final long FAIL_EXPIRE_MINUTES = 15;

    /**
     * 判断账号是否被锁定。
     */
    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(RedisKeyConstants.LOGIN_LOCK_PREFIX + username));
    }

    /**
     * 获取当前失败次数。
     */
    public int getFailCount(String username) {
        String val = redisTemplate.opsForValue().get(RedisKeyConstants.LOGIN_FAIL_PREFIX + username);
        return val == null ? 0 : Integer.parseInt(val);
    }

    /**
     * 是否需要图形验证码（失败 >= 3 次）。
     */
    public boolean isCaptchaRequired(String username) {
        return getFailCount(username) >= CAPTCHA_THRESHOLD;
    }

    /**
     * 登录失败后递增计数，达到阈值则锁定。
     */
    public void recordFailure(String username) {
        String failKey = RedisKeyConstants.LOGIN_FAIL_PREFIX + username;
        Long count = redisTemplate.opsForValue().increment(failKey);
        redisTemplate.expire(failKey, FAIL_EXPIRE_MINUTES, TimeUnit.MINUTES);

        if (count != null && count >= LOCK_THRESHOLD) {
            redisTemplate.opsForValue().set(
                    RedisKeyConstants.LOGIN_LOCK_PREFIX + username,
                    "1",
                    LOCK_MINUTES,
                    TimeUnit.MINUTES
            );
        }
    }

    /**
     * 登录成功后清除失败记录。
     */
    public void clearFailure(String username) {
        redisTemplate.delete(RedisKeyConstants.LOGIN_FAIL_PREFIX + username);
        redisTemplate.delete(RedisKeyConstants.LOGIN_LOCK_PREFIX + username);
    }
}
