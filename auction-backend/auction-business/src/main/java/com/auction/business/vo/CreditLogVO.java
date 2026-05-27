package com.auction.business.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreditLogVO {

    private Long id;
    private Long userId;
    private String eventType;
    private String relatedId;
    private Integer deltaScore;
    private Integer scoreBefore;
    private Integer scoreAfter;
    private String remark;
    private LocalDateTime createdAt;
}
