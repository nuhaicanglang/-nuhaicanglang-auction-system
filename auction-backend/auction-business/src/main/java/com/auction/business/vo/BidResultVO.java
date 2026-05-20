package com.auction.business.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出价结果视图对象。
 * extended/endTime：反狙击延时时填充；deal/status：达到一口价时填充。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidResultVO {

    private Long bidId;
    private BigDecimal currentPrice;
    /** 本次出价是否触发了反狙击延时 */
    private Boolean extended;
    /** 反狙击后的新结束时间 */
    private LocalDateTime endTime;
    /** 是否直接成交（触及一口价） */
    private Boolean deal;
    /** 商品当前状态：3=拍卖中 / 5=已成交 */
    private Integer status;
}
