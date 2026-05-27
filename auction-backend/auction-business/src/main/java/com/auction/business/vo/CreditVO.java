package com.auction.business.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreditVO {

    private Long userId;
    private Integer score;
    private String levelName;
    private Integer status;
    private LocalDateTime lastEventAt;
    private LocalDateTime updatedAt;
}
