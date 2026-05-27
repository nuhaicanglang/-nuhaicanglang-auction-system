package com.auction.business.consumer;

import com.auction.business.service.OrderService;
import com.auction.mq.constant.MqConstants;
import com.auction.mq.message.OrderTimeoutMessage;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单支付超时消费者。
 * 延迟消息到期后，如果订单仍是待支付状态，就自动关闭订单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = MqConstants.QUEUE_ORDER_TIMEOUT)
    public void onMessage(OrderTimeoutMessage msg, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("收到订单支付超时消息: orderId={}", msg.getOrderId());
            orderService.closeTimeoutOrder(msg.getOrderId(), msg.getExpectedPayDeadlineMs());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("订单支付超时消息处理失败: orderId={}", msg.getOrderId(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
