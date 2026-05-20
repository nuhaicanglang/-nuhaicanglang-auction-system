package com.auction.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 拍卖结算延迟消息：审核通过时投递到延迟队列，到期后触发结算。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionSettleMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private Long itemId;
    /** 预期结束时间戳（毫秒），消费者可据此做二次校验 */
    private Long expectedEndTimeMs;
}
