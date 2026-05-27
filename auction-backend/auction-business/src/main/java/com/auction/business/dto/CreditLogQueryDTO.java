package com.auction.business.dto;

import lombok.Data;

@Data
public class CreditLogQueryDTO {

    private Long userId;
    private String eventType;
    private Integer page = 1;
    private Integer size = 20;
}
