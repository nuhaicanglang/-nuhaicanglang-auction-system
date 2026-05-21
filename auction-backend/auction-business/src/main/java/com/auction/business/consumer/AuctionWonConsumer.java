package com.auction.business.consumer;

import com.auction.business.dto.NotifyCreateDTO;
import com.auction.business.service.NotifyService;
import com.auction.mq.constant.MqConstants;
import com.auction.mq.message.AuctionWonMessage;
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
public class AuctionWonConsumer {

    private final NotifyService notifyService;

    @RabbitListener(queues = MqConstants.QUEUE_AUCTION_WON)
    public void onMessage(AuctionWonMessage msg, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            String title = "恭喜您中标";
            String content = "您已中标拍品「" + msg.getItemTitle() + "」，成交价 "
                    + msg.getFinalPrice().toPlainString() + "。";
            notifyService.sendInApp(NotifyCreateDTO.builder()
                    .userId(msg.getWinnerId())
                    .type(2)
                    .title(title)
                    .content(content)
                    .relatedItemId(msg.getItemId())
                    .build());
            log.info("中标通知已发送: itemId={}, winnerId={}", msg.getItemId(), msg.getWinnerId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("中标通知异常: itemId={}, winnerId={}, error={}",
                    msg.getItemId(), msg.getWinnerId(), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
