package com.auction.business.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 钱包调账内部命令。
 * Service 内部统一用这个对象表达“给谁、做什么动作、多少钱、对应哪个业务”。
 */
@Data
public class WalletAdjustCmd {

    private Long userId;
    private String actionType;
    private BigDecimal amount;
    private String bizType;
    private String bizId;
    private Long relatedItemId;
    private Long operatorId;
    private String remark;
    private String idempotentKey;
}
