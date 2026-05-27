package com.auction.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户钱包主表。
 * balance 表示可用余额，frozenBalance 表示已冻结金额，二者相加才是用户账户总资金。
 */
@Data
@TableName("biz_wallet")
public class BizWallet {

    @TableId
    private Long id;

    private Long userId;
    private BigDecimal balance;
    private BigDecimal frozenBalance;
    private Integer status;
    private Long tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Integer version;
}
