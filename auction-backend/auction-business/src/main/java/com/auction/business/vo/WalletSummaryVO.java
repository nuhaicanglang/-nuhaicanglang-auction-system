package com.auction.business.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台钱包汇总对象。
 */
@Data
public class WalletSummaryVO {

    private Long walletCount;
    private BigDecimal totalBalance;
    private BigDecimal totalFrozenBalance;
    private BigDecimal totalAmount;
}
