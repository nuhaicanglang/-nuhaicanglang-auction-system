package com.auction.business.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品列表查询参数。
 */
@Data
public class AuctionItemQueryDTO {

    /** 分类 ID 筛选 */
    private Long categoryId;

    /** 状态筛选（1~7） */
    private Integer status;

    /** 最低价 */
    private BigDecimal priceMin;

    /** 最高价 */
    private BigDecimal priceMax;

    /** 关键字搜索（标题模糊匹配） */
    private String keyword;

    /** 卖家 ID（"我发布的"查询用） */
    private Long sellerId;

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 20;

    /** 排序字段，如 -end_time 表示按结束时间倒序 */
    private String sort;
}
