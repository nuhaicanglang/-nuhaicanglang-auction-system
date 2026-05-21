package com.auction.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体。
 * 拍卖成交后生成订单，后续支付、发货、完成都会围绕订单流转。
 */
@Data
@TableName("biz_order")
public class BizOrder {

    @TableId
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

    /** 1待支付/2已支付/3已发货/4已完成/5已取消/6已关闭 */
    private Integer status;
    private LocalDateTime payDeadline;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime closedAt;
    private String closeReason;

    private Long tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
