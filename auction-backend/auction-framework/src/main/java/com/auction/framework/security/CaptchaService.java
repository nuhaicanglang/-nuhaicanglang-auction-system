package com.auction.framework.security;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.lang.UUID;
import com.auction.common.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务。
 * 使用 Hutool 生成线段干扰验证码，验证码答案存入 Redis（5 分钟过期）。
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final StringRedisTemplate redisTemplate;

    /** 验证码图片宽度 */
    private static final int WIDTH = 160;

    /** 验证码图片高度 */
    private static final int HEIGHT = 60;

    /** 验证码字符数 */
    private static final int CODE_COUNT = 4;

    /** 验证码过期时间（分钟） */
    private static final long EXPIRE_MINUTES = 5;

    /**
     * 生成验证码，返回 uuid 和 base64 图片。
     */
    public Map<String, String> generate() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(WIDTH, HEIGHT, CODE_COUNT, 20);
        String code = captcha.getCode();
        String uuid = UUID.randomUUID().toString(true);

        // 存入 Redis，忽略大小写统一存小写
        redisTemplate.opsForValue().set(
                RedisKeyConstants.CAPTCHA_PREFIX + uuid,
                code.toLowerCase(),
                EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        Map<String, String> result = new HashMap<>(2);
        result.put("uuid", uuid);
        result.put("image", captcha.getImageBase64Data());
        return result;
    }

    /**
     * 校验验证码。校验后无论对错都删除，防止重复使用。
     */
    public boolean verify(String uuid, String code) {
        if (uuid == null || code == null) {
            return false;
        }
        String key = RedisKeyConstants.CAPTCHA_PREFIX + uuid;
        String cachedCode = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        return code.toLowerCase().equals(cachedCode);
    }
}
