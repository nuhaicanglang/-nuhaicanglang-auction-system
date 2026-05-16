package com.auction.business.bid;

import com.auction.business.entity.BizAuctionItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 出价上下文。
 * <p>
 * 在责任链中传递的"信息载体"：
 * 入参字段（itemId/userId/price 等）由 Controller 设置；
 * 加载阶段在第一个校验器中读取商品 → 后续校验器复用。
 * </p>
 */
@Data
@Builder
public class BidContext {

    /** 商品ID */
    private Long itemId;

    /** 出价人ID */
    private Long userId;

    /** 出价金额 */
    private BigDecimal price;

    /** 客户端幂等ID */
    private String requestId;

    /** 客户端IP */
    private String clientIp;

    /** 加载到的商品（在 ItemStatusValidator 中赋值，后续校验器可直接读） */
    private BizAuctionItem item;
}
