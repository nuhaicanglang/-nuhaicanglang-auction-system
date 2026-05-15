package com.auction.business.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 出价请求参数。
 */
@Data
public class BidDTO {

    @NotNull(message = "出价金额不能为空")
    @DecimalMin(value = "0.01", message = "出价金额必须大于0")
    private BigDecimal price;
}
