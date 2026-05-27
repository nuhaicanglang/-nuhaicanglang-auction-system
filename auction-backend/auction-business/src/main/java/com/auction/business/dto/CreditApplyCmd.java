package com.auction.business.dto;

import lombok.Data;

@Data
public class CreditApplyCmd {

    private String eventType;
    private Long userId;
    private String relatedId;
    private Integer deltaScore;
    private String remark;
    private String idempotentKey;
}
