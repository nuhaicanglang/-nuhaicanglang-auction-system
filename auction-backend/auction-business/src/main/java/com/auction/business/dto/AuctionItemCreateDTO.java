package com.auction.business.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布/编辑拍卖商品请求参数。
 */
@Data
public class AuctionItemCreateDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String subtitle;

    private String description;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "封面图不能为空")
    private String coverImage;

    private List<String> images;

    @NotNull(message = "起拍价不能为空")
    @DecimalMin(value = "0.01", message = "起拍价必须大于0")
    private BigDecimal startPrice;

    @NotNull(message = "加价幅度不能为空")
    @DecimalMin(value = "0.01", message = "加价幅度必须大于0")
    private BigDecimal bidIncrement;

    /** 一口价（可选） */
    private BigDecimal buyNowPrice;

    /** 保证金（可选，默认 0） */
    private BigDecimal deposit;

    /** 拍卖时长（分钟），如 1440=24h */
    @NotNull(message = "拍卖时长不能为空")
    @Min(value = 60, message = "拍卖时长至少1小时")
    private Integer duration;

    /** 开拍方式：IMMEDIATE=审核通过即开拍 / SCHEDULED=预约开拍 */
    private String startMode;

    /** 预约开拍时间（startMode=SCHEDULED 时必填） */
    private LocalDateTime scheduledStartTime;
}
