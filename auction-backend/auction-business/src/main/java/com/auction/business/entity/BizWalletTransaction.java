package com.auction.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包资金流水表。
 * 每一次余额变化都写一条流水，用于审计、追踪和对账。
 */
@Data
@TableName("biz_wallet_transaction")
public class BizWalletTransaction {

    @TableId
    private Long id;

    private String transactionNo;
    private Long walletId;
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
    private String idempotentKey;
    private Long tenantId;
    private LocalDateTime createdAt;
}
