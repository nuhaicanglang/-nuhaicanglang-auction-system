package com.auction.business.consumer;

import com.auction.business.dto.NotifyCreateDTO;
import com.auction.business.service.NotifyService;
import com.auction.mq.constant.MqConstants;
import com.auction.mq.message.BidOutbidMessage;
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
public class BidOutbidConsumer {

    private final NotifyService notifyService;

    @RabbitListener(queues = MqConstants.QUEUE_BID_OUTBID)
    public void onMessage(BidOutbidMessage msg, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            String title = "您的出价已被超过";
            String content = "您关注的拍品「" + msg.getItemTitle() + "」当前价已更新为 "
                    + msg.getNewPrice().toPlainString() + "，您的出价 "
                    + msg.getOldPrice().toPlainString() + " 已被超过。";
            notifyService.sendInApp(NotifyCreateDTO.builder()
                    .userId(msg.getOutbidUserId())
                    .type(1)
                    .title(title)
                    .content(content)
                    .relatedItemId(msg.getItemId())
                    .build());
            log.info("被超价通知已发送: itemId={}, userId={}", msg.getItemId(), msg.getOutbidUserId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("被超价通知异常: itemId={}, userId={}, error={}",
                    msg.getItemId(), msg.getOutbidUserId(), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
