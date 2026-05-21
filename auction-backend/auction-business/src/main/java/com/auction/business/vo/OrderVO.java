package com.auction.business.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图对象，用于买家/卖家订单列表和详情返回。
 */
@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long itemId;
    private String itemTitle;
    private String itemCoverImage;
    private Long buyerId;
    private Long sellerId;
    private Long bidId;
    private BigDecimal dealPrice;
    private BigDecimal depositAmount;
    private BigDecimal payAmount;
    private Integer status;
    private String statusText;
    private LocalDateTime payDeadline;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime closedAt;
    private String closeReason;
    private LocalDateTime createdAt;
}
