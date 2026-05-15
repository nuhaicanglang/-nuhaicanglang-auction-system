package com.auction.framework.annotation;

import java.lang.annotation.*;

/**
 * 幂等性注解。
 * 标注在需要防重复提交的接口上。前端在请求头 X-Idempotent-Key 中传递唯一标识，
 * 切面通过 Redis SETNX 保证同一 key 在指定时间窗口内只处理一次。
 *
 * <pre>
 * &#64;Idempotent(expireSeconds = 10)
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /** 幂等 key 过期时间（秒），默认 10 秒 */
    int expireSeconds() default 10;

    /** 重复请求时返回的提示信息 */
    String message() default "请勿重复提交";
}
