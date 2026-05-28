package com.auction.business.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 热门商品 TOP10。
 */
@Data
public class ItemTopVO {
    private Long itemId;
    private String title;
    private Long categoryId;
    private BigDecimal currentPrice;
    private BigDecimal finalPrice;
    private Integer bidCount;
    private Integer viewCount;
    private Integer status;
}
