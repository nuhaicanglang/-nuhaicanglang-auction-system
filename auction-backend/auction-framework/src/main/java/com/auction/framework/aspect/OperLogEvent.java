package com.auction.framework.aspect;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志事件对象。
 * LogAspect 采集到的信息封装成此对象，由 OperLogHandler 接口的实现方负责持久化。
 * 这样 framework 层不需要直接依赖 system 层的 Entity/Mapper。
 */
@Data
public class OperLogEvent {

    private String traceId;
    private String module;
    private String businessType;
    private String description;
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
    private Integer costMs;
    private LocalDateTime createdAt;
}
