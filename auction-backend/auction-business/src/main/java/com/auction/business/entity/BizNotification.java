package com.auction.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_notification")
public class BizNotification {

    @TableId
    private Long id;

    private Long userId;
    private Integer type;
    private String title;
    private String content;
    private Long relatedItemId;
    private Integer isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
