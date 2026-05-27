package com.auction.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单支付超时延迟消息。
 * 订单创建后投递，到支付截止时间仍未支付时由消费者关闭订单。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeoutMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long orderId;
    /** 预期支付截止时间戳（毫秒），消费者用它判断旧消息是否应跳过 */
    private Long expectedPayDeadlineMs;
}
