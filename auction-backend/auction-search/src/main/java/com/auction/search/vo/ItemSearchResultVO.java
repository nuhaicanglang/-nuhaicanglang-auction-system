package com.auction.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 搜索结果聚合 VO：分页 + 命中列表 + 分面聚合。
 */
@Data
public class ItemSearchResultVO {

    private Long total;
    private Integer page;
    private Integer size;

    private List<ItemHitVO> items;

    /** 分类聚合（按 categoryId 计数） */
    private List<FacetVO> categoryFacets;

    /** 状态聚合（按 status 计数） */
    private List<FacetVO> statusFacets;

    /** 价格区间聚合（key 为 "0-100"/"100-500" 等） */
    private List<FacetVO> priceFacets;
}
