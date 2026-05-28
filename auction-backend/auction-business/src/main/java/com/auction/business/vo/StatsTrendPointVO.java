package com.auction.business.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 趋势图单日数据点。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsTrendPointVO {
    /** yyyy-MM-dd */
    private String date;
    /** 新增商品数 */
    private Long itemCount;
    /** 新增订单数 */
    private Long orderCount;
    /** 当日成交金额 */
    private BigDecimal dealAmount;
}
