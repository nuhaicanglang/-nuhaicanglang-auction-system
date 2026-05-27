package com.auction.business.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreditAdjustDTO {

    @NotNull(message = "调整分值不能为空")
    @Min(value = -100, message = "单次最多扣100分")
    @Max(value = 100, message = "单次最多加100分")
    private Integer deltaScore;

    @NotBlank(message = "调整原因不能为空")
    private String remark;

    private String idempotentKey;
}
