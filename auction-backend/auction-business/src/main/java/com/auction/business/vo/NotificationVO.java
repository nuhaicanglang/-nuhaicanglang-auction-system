package com.auction.business.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {

    private Long id;
    private Integer type;
    private String title;
    private String content;
    private Long relatedItemId;
    private Integer isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
