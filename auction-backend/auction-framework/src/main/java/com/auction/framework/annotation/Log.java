package com.auction.framework.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解。
 * 标记在 Controller 方法上，AOP 切面会自动采集请求参数、响应数据、耗时等信息，
 * 异步写入 sys_oper_log 表。
 *
 * <pre>
 * &#64;Log(module = "用户管理", businessType = "EDIT", description = "修改用户状态")
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /** 所属模块，如 "用户管理"、"拍卖" */
    String module() default "";

    /** 业务类型，如 NEW / EDIT / DELETE / QUERY / EXPORT */
    String businessType() default "";

    /** 操作描述 */
    String description() default "";
}
