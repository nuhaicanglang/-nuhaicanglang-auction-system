package com.auction.search.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聚合分桶。key 为字符串方便分类/状态/价格区间复用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacetVO {
    private String key;
    private Long count;
}
