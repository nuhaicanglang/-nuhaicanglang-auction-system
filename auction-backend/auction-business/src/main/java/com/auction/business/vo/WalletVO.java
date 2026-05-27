package com.auction.business.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包展示对象。
 */
@Data
public class WalletVO {

    private Long id;
    private Long userId;
    private BigDecimal balance;
    private BigDecimal frozenBalance;
    private BigDecimal totalAmount;
    private Integer status;
    private LocalDateTime updatedAt;
}
