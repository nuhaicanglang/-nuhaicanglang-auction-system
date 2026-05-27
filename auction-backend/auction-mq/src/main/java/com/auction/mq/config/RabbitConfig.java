package com.auction.mq.config;

import com.auction.mq.constant.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 核心配置：声明交换机、队列、绑定关系。
 *
 * <p>架构设计：
 * <pre>
 *   Producer
 *     │
 *     ├─ auction.direct ──RK:bid.persist──→ bid.persist.queue
 *     │                  ──RK:bid.outbid──→ bid.outbid.queue
 *     │                  ──RK:auction.won──→ auction.won.queue
 *     │                  ──RK:auction.delay─→ auction.delay.queue
 *     │                                         │ (TTL 到期)
 *     │                                         ▼
 *     └─ auction.dlx ────RK:auction.settle──→ auction.settle.queue
 * </pre>
 *
 * <p>延迟队列原理：消息发送到 auction.delay.queue 时附带 per-message TTL，
 * 到期后自动转发到死信交换机 auction.dlx，再路由到 auction.settle.queue。
 */
@Configuration
public class RabbitConfig {

    // ==================== 消息转换器 ====================

    /**
     * 使用 Jackson JSON 序列化消息体，替代默认的 Java 序列化。
     * 好处：消息可读、跨语言兼容、体积更小。
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 自定义 RabbitTemplate：设置 JSON 转换器 + 开启 Confirm 回调。
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        // mandatory=true：消息无法路由时回退给发送方，而不是静默丢弃
        template.setMandatory(true);
        return template;
    }

    // ==================== 交换机 ====================

    /** 主交换机：direct 类型，持久化 */
    @Bean
    public DirectExchange auctionDirectExchange() {
        return ExchangeBuilder.directExchange(MqConstants.EXCHANGE_DIRECT)
                .durable(true)
                .build();
    }

    /** 死信交换机：direct 类型，持久化 */
    @Bean
    public DirectExchange auctionDlxExchange() {
        return ExchangeBuilder.directExchange(MqConstants.EXCHANGE_DLX)
                .durable(true)
                .build();
    }

    // ==================== 队列 ====================

    /** 出价持久化队列：持久化、非排他、非自动删除 */
    @Bean
    public Queue bidPersistQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_BID_PERSIST).build();
    }

    /** 被超价通知队列 */
    @Bean
    public Queue bidOutbidQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_BID_OUTBID).build();
    }

    /** 拍卖结算队列（也是延迟队列的最终目的地） */
    @Bean
    public Queue auctionSettleQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_AUCTION_SETTLE).build();
    }

    /** 中标通知队列 */
    @Bean
    public Queue auctionWonQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_AUCTION_WON).build();
    }

    /**
     * 延迟队列：消息到期后自动转发到死信交换机 → 结算队列。
     * x-dead-letter-exchange: 到期后消息发往的交换机
     * x-dead-letter-routing-key: 到期后使用的路由键
     */
    @Bean
    public Queue auctionDelayQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_AUCTION_DELAY)
                .deadLetterExchange(MqConstants.EXCHANGE_DLX)
                .deadLetterRoutingKey(MqConstants.RK_AUCTION_SETTLE)
                .build();
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_ORDER_TIMEOUT).build();
    }

    @Bean
    public Queue orderTimeoutDelayQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_ORDER_TIMEOUT_DELAY)
                .deadLetterExchange(MqConstants.EXCHANGE_DLX)
                .deadLetterRoutingKey(MqConstants.RK_ORDER_TIMEOUT)
                .build();
    }

    @Bean
    public Queue creditEventQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_CREDIT_EVENT).build();
    }

    /** ES 商品同步队列 */
    @Bean
    public Queue itemSyncQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_ITEM_SYNC).build();
    }

    // ==================== 绑定关系 ====================

    @Bean
    public Binding bindBidPersist(Queue bidPersistQueue, DirectExchange auctionDirectExchange) {
        return BindingBuilder.bind(bidPersistQueue)
                .to(auctionDirectExchange)
                .with(MqConstants.RK_BID_PERSIST);
    }

    @Bean
    public Binding bindBidOutbid(Queue bidOutbidQueue, DirectExchange auctionDirectExchange) {
        return BindingBuilder.bind(bidOutbidQueue)
                .to(auctionDirectExchange)
                .with(MqConstants.RK_BID_OUTBID);
    }

    @Bean
    public Binding bindAuctionWon(Queue auctionWonQueue, DirectExchange auctionDirectExchange) {
        return BindingBuilder.bind(auctionWonQueue)
                .to(auctionDirectExchange)
                .with(MqConstants.RK_AUCTION_WON);
    }

    /** 主交换机 → 延迟队列（生产者发消息到此队列等待 TTL） */
    @Bean
    public Binding bindAuctionDelay(Queue auctionDelayQueue, DirectExchange auctionDirectExchange) {
        return BindingBuilder.bind(auctionDelayQueue)
                .to(auctionDirectExchange)
                .with(MqConstants.RK_AUCTION_DELAY);
    }

    @Bean
    public Binding bindOrderTimeoutDelay(Queue orderTimeoutDelayQueue, DirectExchange auctionDirectExchange) {
        return BindingBuilder.bind(orderTimeoutDelayQueue)
                .to(auctionDirectExchange)
                .with(MqConstants.RK_ORDER_TIMEOUT_DELAY);
    }

    @Bean
    public Binding bindCreditEvent(Queue creditEventQueue, DirectExchange auctionDirectExchange) {
        return BindingBuilder.bind(creditEventQueue)
                .to(auctionDirectExchange)
                .with(MqConstants.RK_CREDIT_EVENT);
    }

    @Bean
    public Binding bindItemSync(Queue itemSyncQueue, DirectExchange auctionDirectExchange) {
        return BindingBuilder.bind(itemSyncQueue)
                .to(auctionDirectExchange)
                .with(MqConstants.RK_ITEM_SYNC);
    }

    /** 死信交换机 → 结算队列（延迟队列消息到期后最终到达此处） */
    @Bean
    public Binding bindAuctionSettle(Queue auctionSettleQueue, DirectExchange auctionDlxExchange) {
        return BindingBuilder.bind(auctionSettleQueue)
                .to(auctionDlxExchange)
                .with(MqConstants.RK_AUCTION_SETTLE);
    }

    @Bean
    public Binding bindOrderTimeout(Queue orderTimeoutQueue, DirectExchange auctionDlxExchange) {
        return BindingBuilder.bind(orderTimeoutQueue)
                .to(auctionDlxExchange)
                .with(MqConstants.RK_ORDER_TIMEOUT);
    }
}
