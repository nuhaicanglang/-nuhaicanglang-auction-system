package com.auction.system.dto;

import lombok.Data;

/**
 * 操作日志查询参数。
 */
@Data
public class OperLogQueryDTO {

    private String module;

    private String businessType;

    private String keyword;

    private String operUserName;

    private Integer status;

    private Integer page = 1;

    private Integer size = 20;
}
