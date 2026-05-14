package com.auction.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志标记注解。
 * 后续配合 AOP 使用，用来记录管理员或用户的重要操作。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    /**
     * 操作说明，例如“创建拍品”“修改用户状态”。
     */
    String value();
}
