package com.auction.mq.constant;

/**
 * RabbitMQ 常量定义：交换机、队列、路由键集中管理。
 * 生产者发送消息和消费者监听队列都引用这里的常量，避免魔法字符串。
 */
public final class MqConstants {

    private MqConstants() {}

    // ==================== 交换机 ====================

    /** 主交换机（direct 类型），路由所有业务消息 */
    public static final String EXCHANGE_DIRECT = "auction.direct";

    /** 死信交换机（direct 类型），接收延迟队列过期消息 */
    public static final String EXCHANGE_DLX = "auction.dlx";

    // ==================== 队列 ====================

    /** 出价持久化队列：消费者从 Redis 出价队列读取并写入 MySQL */
    public static final String QUEUE_BID_PERSIST = "bid.persist.queue";

    /** 被超价通知队列：当用户被超价时，发送站内信/WS通知 */
    public static final String QUEUE_BID_OUTBID = "bid.outbid.queue";

    /** 拍卖结算队列：延迟到期后处理结算（确定中标人、生成订单） */
    public static final String QUEUE_AUCTION_SETTLE = "auction.settle.queue";

    /** 中标通知队列：结算完成后通知中标者 */
    public static final String QUEUE_AUCTION_WON = "auction.won.queue";

    /** 延迟队列：商品开拍时投递 TTL 消息，到期后转入结算队列 */
    public static final String QUEUE_AUCTION_DELAY = "auction.delay.queue";

    /** 订单支付超时队列：延迟到期后关闭未支付订单 */
    public static final String QUEUE_ORDER_TIMEOUT = "order.timeout.queue";

    /** 订单支付超时延迟队列：订单创建后投递 TTL 消息 */
    public static final String QUEUE_ORDER_TIMEOUT_DELAY = "order.timeout.delay.queue";

    // ==================== 路由键 ====================

    public static final String RK_BID_PERSIST = "bid.persist";
    public static final String RK_BID_OUTBID = "bid.outbid";
    public static final String RK_AUCTION_SETTLE = "auction.settle";
    public static final String RK_AUCTION_WON = "auction.won";
    public static final String RK_AUCTION_DELAY = "auction.delay";
    public static final String RK_ORDER_TIMEOUT = "order.timeout";
    public static final String RK_ORDER_TIMEOUT_DELAY = "order.timeout.delay";
}
