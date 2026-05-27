package com.auction.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_review")
public class BizReview {

    @TableId
    private Long id;

    private Long orderId;
    private Long itemId;
    private Long reviewerId;
    private Long revieweeId;
    private String roleType;
    private Integer score;
    private String content;
    private Integer status;
    private Long tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
