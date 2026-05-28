package com.auction.search.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 搜索结果单条命中。
 */
@Data
public class ItemHitVO {

    private Long id;
    private String title;
    /** 高亮后的标题（含 <em> 标签）；无高亮时与 title 相同 */
    private String highlightTitle;
    private String subtitle;
    private String highlightSubtitle;
    private Long categoryId;
    private String coverImage;
    private Long sellerId;
    private BigDecimal currentPrice;
    private BigDecimal buyNowPrice;
    private Integer bidCount;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
