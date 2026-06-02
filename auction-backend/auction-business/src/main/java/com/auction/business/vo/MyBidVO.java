package com.auction.business.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户维度竞拍流水视图对象。
 */
@Data
public class MyBidVO {

    private Long id;
    private Long itemId;
    private String itemTitle;
    private String itemCoverImage;
    private Long sellerId;

    private BigDecimal myBidPrice;
    private BigDecimal currentPrice;
    private BigDecimal buyNowPrice;

    private LocalDateTime bidTime;
    private LocalDateTime endTime;

    private Integer bidType;
    private Integer bidStatus;
    private Integer itemStatus;
    private String itemStatusText;

    private String resultCode;
    private String resultText;
}
