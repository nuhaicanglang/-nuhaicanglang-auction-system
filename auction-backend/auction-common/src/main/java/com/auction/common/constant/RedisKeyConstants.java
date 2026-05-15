package com.auction.common.constant;

/**
 * Redis Key 前缀常量。
 * 统一管理 Key 命名，避免散落在各处导致命名冲突。
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {}

    /** 图形验证码：captcha:{uuid} → code，TTL 5 分钟 */
    public static final String CAPTCHA_PREFIX = "captcha:";

    /** 登录失败计数：login:fail:{username} → count，TTL 15 分钟 */
    public static final String LOGIN_FAIL_PREFIX = "login:fail:";

    /** 账号锁定标识：login:lock:{username} → 1，TTL 15 分钟 */
    public static final String LOGIN_LOCK_PREFIX = "login:lock:";

    /** JWT 黑名单：jwt:blacklist:{jti} → 1，TTL = token 剩余有效期 */
    public static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";

    /** Refresh Token：refresh:{userId} → refreshToken，TTL 7 天 */
    public static final String REFRESH_TOKEN_PREFIX = "refresh:";
}
