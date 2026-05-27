package com.auction.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付流水。
 * Day 22 先实现模拟钱包支付，后续可扩展支付宝、微信等真实支付渠道。
 */
@Data
@TableName("biz_payment")
public class BizPayment {

    @TableId
    private Long id;

    private String paymentNo;
    private Long orderId;
    private String orderNo;
    private Long payerId;
    private BigDecimal amount;
    private String payMethod;
    private Integer status;
    private LocalDateTime paidAt;
    private String idempotentKey;
    private String remark;
    private Long tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
