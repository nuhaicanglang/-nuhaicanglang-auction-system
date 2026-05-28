package com.auction.search.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品搜索查询参数。
 * 搭配 ES bool query + filter + highlight + 聚合使用。
 */
@Data
public class ItemSearchQueryDTO {

    /** 关键词（title/subtitle 分词匹配；为空则不参与 must） */
    private String keyword;

    /** 分类筛选 */
    private Long categoryId;

    /** 价格区间下限（含） */
    private BigDecimal minPrice;

    /** 价格区间上限（含） */
    private BigDecimal maxPrice;

    /** 状态筛选：2待开/3进行/4已结/5已成/6流拍；为空则只返回 status>=2 的可见商品 */
    private Integer status;

    /**
     * 排序字段：relevance(默认相关度) / endTime / currentPrice / bidCount / createdAt。
     */
    private String sort;

    /** 排序方向：asc / desc（默认 desc） */
    private String order;

    /** 是否记录到搜索历史（默认 true，仅登录用户生效） */
    private Boolean saveHistory = Boolean.TRUE;

    private Integer page = 1;
    private Integer size = 10;
}
