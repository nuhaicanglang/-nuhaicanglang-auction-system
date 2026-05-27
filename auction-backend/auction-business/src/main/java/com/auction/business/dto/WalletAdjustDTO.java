package com.auction.business.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理员钱包调账请求。
 * 管理员调账必须输入本人密码，避免误操作或越权操作。
 */
@Data
public class WalletAdjustDTO {

    @NotBlank(message = "调账动作不能为空")
    private String actionType;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "管理员密码不能为空")
    private String adminPassword;

    private String remark;
    private String idempotentKey;
}
