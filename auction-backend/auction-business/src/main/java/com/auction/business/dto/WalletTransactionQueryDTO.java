package com.auction.business.dto;

import lombok.Data;

/**
 * 钱包流水查询参数。
 */
@Data
public class WalletTransactionQueryDTO {

    private Long userId;
    private String actionType;
    private String bizType;
    private Integer page = 1;
    private Integer size = 20;
}
