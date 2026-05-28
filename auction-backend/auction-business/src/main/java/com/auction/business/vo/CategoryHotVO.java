package com.auction.business.vo;

import lombok.Data;

/**
 * 热门分类。按在售/进行中商品数量降序排列。
 */
@Data
public class CategoryHotVO {
    private Long categoryId;
    private String categoryName;
    private Long itemCount;
}
