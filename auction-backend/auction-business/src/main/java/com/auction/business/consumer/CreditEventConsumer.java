package com.auction.business.consumer;

import com.auction.business.service.CreditService;
import com.auction.mq.constant.MqConstants;
import com.auction.mq.message.CreditEventMessage;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditEventConsumer {

    private final CreditService creditService;

    @RabbitListener(queues = MqConstants.QUEUE_CREDIT_EVENT)
    public void onMessage(CreditEventMessage msg, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            creditService.applyEvent(msg.getEventType(), msg.getUserId(), msg.getRelatedId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("信用事件处理失败: eventType={}, userId={}, relatedId={}",
                    msg.getEventType(), msg.getUserId(), msg.getRelatedId(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
