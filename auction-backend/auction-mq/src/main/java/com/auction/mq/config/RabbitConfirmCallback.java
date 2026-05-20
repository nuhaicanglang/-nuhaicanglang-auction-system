package com.auction.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * RabbitMQ 发布确认回调：
 * 1. ConfirmCallback — 消息是否成功到达交换机
 * 2. ReturnsCallback — 消息到达交换机但无法路由到队列时回退
 *
 * 两个回调都仅打印日志，生产环境可在此处做补偿（如重发、告警）。
 */
@Slf4j
@Component
public class RabbitConfirmCallback implements RabbitTemplate.ConfirmCallback,
        RabbitTemplate.ReturnsCallback {

    private final RabbitTemplate rabbitTemplate;

    public RabbitConfirmCallback(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** 启动时把自己注册到 RabbitTemplate */
    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnsCallback(this);
    }

    /**
     * 消息到达交换机的确认回调。
     * ack=true 表示交换机已收到；ack=false 表示交换机拒绝（极少见，通常是 Broker 内部错误）。
     */
    @Override
    public void confirm(org.springframework.amqp.rabbit.connection.CorrelationData correlationData,
                        boolean ack, String cause) {
        if (ack) {
            log.debug("消息投递到交换机成功: id={}", correlationData != null ? correlationData.getId() : "null");
        } else {
            log.error("消息投递到交换机失败: id={}, cause={}",
                    correlationData != null ? correlationData.getId() : "null", cause);
        }
    }

    /**
     * 消息无法路由到任何队列时的回退回调（mandatory=true 时生效）。
     * 常见原因：routing key 写错、队列未声明。
     */
    @Override
    public void returnedMessage(ReturnedMessage returned) {
        log.warn("消息回退: exchange={}, routingKey={}, replyCode={}, replyText={}, body={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText(),
                new String(returned.getMessage().getBody()));
    }
}
