package com.auction.business.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付结果展示对象。
 */
@Data
public class PaymentVO {

    private Long id;
    private String paymentNo;
    private Long orderId;
    private String orderNo;
    private Long payerId;
    private BigDecimal amount;
    private String payMethod;
    private Integer status;
    private String statusText;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
