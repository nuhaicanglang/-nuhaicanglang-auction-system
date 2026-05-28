package com.auction.business.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理仪表盘 - 概览。
 * 统计今日核心指标 + 累计指标。
 */
@Data
public class StatsOverviewVO {

    /** 今日新增用户 */
    private Long todayNewUsers;
    /** 今日新增商品 */
    private Long todayNewItems;
    /** 今日新增订单 */
    private Long todayNewOrders;
    /** 今日成交金额（订单 status>=2 的 pay_amount 之和） */
    private BigDecimal todayDealAmount;

    /** 累计用户 */
    private Long totalUsers;
    /** 累计商品 */
    private Long totalItems;
    /** 累计订单 */
    private Long totalOrders;
    /** 累计成交金额 */
    private BigDecimal totalDealAmount;

    /** 进行中拍卖数（status=3） */
    private Long ongoingAuctions;
    /** 待支付订单数（status=1） */
    private Long pendingPayments;
}
