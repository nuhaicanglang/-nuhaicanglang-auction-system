package com.auction.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拍卖商品实体。
 * 对应 biz_auction_item 表，是整个系统的核心业务表。
 * status 字段值：1待审/2待开/3进行/4已结/5已成/6流拍/7下架。
 */
@Data
@TableName("biz_auction_item")
public class BizAuctionItem {

    @TableId
    private Long id;

    private String title;
    private String subtitle;
    private String description;
    private Long categoryId;
    private String categoryPath;
    private String coverImage;

    /** JSON 字符串 ["url1","url2",...] */
    private String images;

    private Long sellerId;

    /** 1英式 / 2荷兰式（预留） */
    private Integer auctionType;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private BigDecimal bidIncrement;
    private BigDecimal buyNowPrice;
    private BigDecimal deposit;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime actualEndTime;

    /** 1待审/2待开/3进行/4已结/5已成/6流拍/7下架 */
    private Integer status;
    /** 0待审/1通过/2驳回 */
    private Integer auditStatus;
    private String auditRemark;
    private Long auditBy;
    private LocalDateTime auditAt;

    private Long winnerId;
    private BigDecimal finalPrice;

    private Integer bidCount;
    private Integer viewCount;
    private Integer favoriteCount;

    private Integer isAntiSnipe;
    private Integer antiSnipeMin;

    private Long tenantId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}
