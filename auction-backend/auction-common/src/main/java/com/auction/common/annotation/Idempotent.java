package com.auction.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等控制标记注解。
 * 常用于下单、支付、出价等不能重复提交的接口。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等 key，为空时后续切面可以按请求路径、用户ID、请求参数自动生成。
     */
    String key() default "";

    /**
     * 幂等记录的过期时间，单位为秒。
     */
    int expireSeconds() default 300;
}
