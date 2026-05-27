package com.auction.business.dto;

import lombok.Data;

@Data
public class ReviewQueryDTO {

    private Long userId;
    private Long itemId;
    private Integer page = 1;
    private Integer size = 20;
}
