package com.auction.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysOperLogVO {

    private Long id;
    private String traceId;
    private String module;
    private String businessType;
    private String description;
    private String method;
    private String requestUrl;
    private String requestMethod;
    private Long operUserId;
    private String operUserName;
    private String operIp;
    private Integer status;
    private String errorMsg;
    private Integer costMs;
    private LocalDateTime createdAt;
}
