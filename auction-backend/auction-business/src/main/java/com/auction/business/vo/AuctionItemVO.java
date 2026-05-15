package com.auction.business.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 拍卖商品视图对象，用于列表/详情返回。
 */
@Data
public class AuctionItemVO {

    private Long id;
    private String title;
    private String subtitle;
    private String description;
    private Long categoryId;
    private String categoryPath;
    private String coverImage;
    private List<String> images;

    /** 卖家信息 */
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;

    private Integer auctionType;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private BigDecimal bidIncrement;
    private BigDecimal buyNowPrice;
    private BigDecimal deposit;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime actualEndTime;

    private Integer status;
    private String statusText;

    private Integer bidCount;
    private Integer viewCount;
    private Integer favoriteCount;

    private LocalDateTime createdAt;
}
