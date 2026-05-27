package com.auction.business.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewVO {

    private Long id;
    private Long orderId;
    private Long itemId;
    private Long reviewerId;
    private Long revieweeId;
    private String roleType;
    private Integer score;
    private String content;
    private Integer status;
    private LocalDateTime createdAt;
}
