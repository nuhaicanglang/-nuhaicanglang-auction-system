package com.auction.business.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包流水展示对象。
 */
@Data
public class WalletTransactionVO {

    private Long id;
    private String transactionNo;
    private Long userId;
    private String actionType;
    private Integer direction;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal frozenBefore;
    private BigDecimal frozenAfter;
    private String bizType;
    private String bizId;
    private Long relatedItemId;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
