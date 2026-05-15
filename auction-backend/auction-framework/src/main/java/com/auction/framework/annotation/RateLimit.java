package com.auction.framework.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解。
 * 基于 Redis Lua 脚本实现滑动窗口限流，标注在 Controller 方法上。
 *
 * <pre>
 * &#64;RateLimit(key = "login", count = 10, period = 60)
 * </pre>
 * 表示 60 秒内最多允许 10 次请求（按 key + 用户IP 限流）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 限流标识前缀 */
    String key() default "";

    /** 时间窗口内最大请求数 */
    int count() default 100;

    /** 时间窗口，单位秒 */
    int period() default 60;
}
