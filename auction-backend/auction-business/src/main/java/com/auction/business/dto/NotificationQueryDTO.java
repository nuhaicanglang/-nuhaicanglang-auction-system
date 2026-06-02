package com.auction.business.dto;

import lombok.Data;

/**
 * 站内信查询参数。
 */
@Data
public class NotificationQueryDTO {

    private Integer isRead;

    private Integer page = 1;

    private Integer size = 20;
}
