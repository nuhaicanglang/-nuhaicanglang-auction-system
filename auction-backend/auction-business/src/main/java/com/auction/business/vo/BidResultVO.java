package com.auction.business.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 出价结果视图对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidResultVO {

    private Long bidId;
    private BigDecimal currentPrice;
}
