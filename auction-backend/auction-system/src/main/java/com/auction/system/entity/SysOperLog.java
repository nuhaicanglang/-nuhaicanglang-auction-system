package com.auction.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体。
 * 记录用户对业务资源的增删改操作，配合 @Log 切面自动采集。
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId
    private Long id;

    /** 链路追踪ID，与 TraceIdFilter 生成的保持一致 */
    private String traceId;

    /** 业务模块名，如 system/auction */
    private String module;

    /** 业务类型：NEW/EDIT/DELETE/QUERY/IMPORT/EXPORT 等 */
    private String businessType;

    /** 操作描述 */
    private String description;

    /** 调用的 Controller 类名 + 方法名 */
    private String method;

    private String requestUrl;

    private String requestMethod;

    private String requestParams;

    private String responseData;

    private Long operUserId;

    private String operUserName;

    private String operIp;

    private String userAgent;

    /** 0成功 / 1失败 */
    private Integer status;

    private String errorMsg;

    /** 接口耗时毫秒 */
    private Integer costMs;

    private LocalDateTime createdAt;
}
