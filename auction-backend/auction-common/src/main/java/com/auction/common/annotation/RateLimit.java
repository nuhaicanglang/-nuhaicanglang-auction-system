package com.auction.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流标记注解。
 * 后续配合 Redis 和 AOP 使用，用来限制接口在一定时间窗口内的访问次数。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流 key，用来区分不同接口或不同用户。
     */
    String key();

    /**
     * 时间窗口内允许的最大请求次数。
     */
    int limit();

    /**
     * 时间窗口长度，单位为秒。
     */
    int windowSeconds();
}
