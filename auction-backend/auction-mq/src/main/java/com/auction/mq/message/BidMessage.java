package com.auction.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 出价消息体：BidServiceImpl 发送到 MQ，BidPersistConsumer 消费后落库。
 * 包含写入 biz_bid 表和更新 biz_auction_item 所需的全部字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 出价记录ID（雪花算法预生成） */
    private Long bidId;
    /** 商品ID */
    private Long itemId;
    /** 出价人ID */
    private Long bidderId;
    /** 出价金额 */
    private BigDecimal bidPrice;
    /** 出价时间戳（毫秒） */
    private Long bidTimeMs;
    /** 出价类型：1正常/2自动/3一口价 */
    private Integer bidType;
    /** 客户端IP */
    private String clientIp;
    /** 客户端幂等ID（biz_bid.client_request_id 唯一索引保证去重） */
    private String clientRequestId;
}
