package com.auction.business.consumer;

import com.auction.business.entity.BizAuctionItem;
import com.auction.business.entity.BizBid;
import com.auction.business.mapper.BizBidMapper;
import com.auction.business.service.AuctionItemService;
import com.auction.mq.constant.MqConstants;
import com.auction.mq.message.BidOutbidMessage;
import com.auction.mq.message.BidMessage;
import com.auction.mq.message.ItemSyncMessage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 出价持久化消费者：监听 bid.persist.queue，将出价记录写入 MySQL。
 *
 * <p>流程：
 * 1. 从 MQ 收到 BidMessage
 * 2. 插入 biz_bid 表（client_request_id 唯一索引保证幂等）
 * 3. 更新 biz_auction_item 的 bid_count 和 current_price
 * 4. 手动 ack 确认消费成功
 *
 * <p>幂等保证：若 client_request_id 重复（DuplicateKeyException），
 * 直接 ack 丢弃，不会导致重复记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BidPersistConsumer {

    private final BizBidMapper bidMapper;
    private final AuctionItemService auctionItemService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = MqConstants.QUEUE_BID_PERSIST)
    public void onMessage(BidMessage msg, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.debug("收到出价消息: bidId={}, itemId={}, price={}",
                    msg.getBidId(), msg.getItemId(), msg.getBidPrice());

            BizBid previousTopBid = bidMapper.selectOne(
                    new LambdaQueryWrapper<BizBid>()
                            .eq(BizBid::getItemId, msg.getItemId())
                            .eq(BizBid::getStatus, 1)
                            .ne(BizBid::getBidderId, msg.getBidderId())
                            .orderByDesc(BizBid::getBidPrice)
                            .last("LIMIT 1"));

            // 1. 插入 biz_bid（幂等：client_request_id 唯一索引）
            BizBid bid = new BizBid();
            bid.setId(msg.getBidId());
            bid.setItemId(msg.getItemId());
            bid.setBidderId(msg.getBidderId());
            bid.setBidPrice(msg.getBidPrice());
            bid.setBidTime(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(msg.getBidTimeMs()), ZoneId.systemDefault()));
            bid.setBidType(msg.getBidType());
            bid.setStatus(1); // 有效
            bid.setClientIp(msg.getClientIp());
            bid.setClientRequestId(msg.getClientRequestId());
            bid.setTenantId(0L);

            try {
                bidMapper.insert(bid);
            } catch (DuplicateKeyException e) {
                // 幂等去重：该出价已入库，直接 ack
                log.info("出价已存在，幂等跳过: requestId={}", msg.getClientRequestId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            BizAuctionItem item = auctionItemService.getById(msg.getItemId());
            if (item == null || !Integer.valueOf(3).equals(item.getStatus())) {
                log.info("出价补充落库完成，商品已非拍卖中，跳过价格回写: itemId={}, bidId={}",
                        msg.getItemId(), msg.getBidId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 累加 bid_count、更新 current_price（以 Redis 为准，但 DB 记录审计）
            auctionItemService.lambdaUpdate()
                    .eq(BizAuctionItem::getId, msg.getItemId())
                    .set(BizAuctionItem::getCurrentPrice, msg.getBidPrice())
                    .setSql("bid_count = bid_count + 1")
                    .update();

            if (previousTopBid != null && msg.getBidPrice().compareTo(previousTopBid.getBidPrice()) > 0) {
                BidOutbidMessage outbidMsg = BidOutbidMessage.builder()
                        .itemId(msg.getItemId())
                        .itemTitle(item != null ? item.getTitle() : String.valueOf(msg.getItemId()))
                        .outbidUserId(previousTopBid.getBidderId())
                        .newBidderId(msg.getBidderId())
                        .oldPrice(previousTopBid.getBidPrice())
                        .newPrice(msg.getBidPrice())
                        .bidId(msg.getBidId())
                        .build();
                rabbitTemplate.convertAndSend(
                        MqConstants.EXCHANGE_DIRECT,
                        MqConstants.RK_BID_OUTBID,
                        outbidMsg);
            }

            log.info("出价落库成功: bidId={}, itemId={}, price={}",
                    msg.getBidId(), msg.getItemId(), msg.getBidPrice());

            // 3. 同步 ES 索引（价格、出价数变更）
            rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_DIRECT, MqConstants.RK_ITEM_SYNC,
                    ItemSyncMessage.builder().itemId(msg.getItemId()).action("UPSERT").build());

            // 4. 手动 ack
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("出价落库异常: bidId={}, error={}", msg.getBidId(), e.getMessage(), e);
            // nack + 不重新入队（由 DLX 处理或人工介入）
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
