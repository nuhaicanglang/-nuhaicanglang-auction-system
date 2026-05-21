package com.auction.business.consumer;

import com.auction.business.entity.BizAuctionItem;
import com.auction.business.entity.BizBid;
import com.auction.business.mapper.BizBidMapper;
import com.auction.business.service.AuctionItemService;
import com.auction.business.service.OrderService;
import com.auction.framework.redis.RedisKey;
import com.auction.framework.websocket.WsPusher;
import com.auction.mq.constant.MqConstants;
import com.auction.mq.message.AuctionSettleMessage;
import com.auction.mq.message.AuctionWonMessage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 拍卖结算消费者：监听 auction.settle.queue（延迟队列到期后由 DLX 路由到此）。
 *
 * <p>结算流程：
 * 1. 校验商品是否仍在拍卖中（防止重复结算、提前成交等）
 * 2. 查询最高有效出价 → 确定中标人
 * 3. 有出价 → 标记已成交(status=5)，设 winner/finalPrice
 * 4. 无出价 → 标记流拍(status=6)
 * 5. 清理 Redis 缓存，广播状态变化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionSettleConsumer {

    private final AuctionItemService auctionItemService;
    private final BizBidMapper bidMapper;
    private final WsPusher wsPusher;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final OrderService orderService;

    @RabbitListener(queues = MqConstants.QUEUE_AUCTION_SETTLE)
    public void onMessage(AuctionSettleMessage msg, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        Long itemId = msg.getItemId();
        try {
            log.info("收到结算消息: itemId={}", itemId);

            // 1. 加载商品
            BizAuctionItem item = auctionItemService.getById(itemId);
            if (item == null) {
                log.warn("结算失败: 商品不存在 itemId={}", itemId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 幂等校验：只处理拍卖中(status=3)的商品
            //    如果已被一口价成交(5)、下架(7)等，直接跳过
            if (item.getStatus() != 3) {
                log.info("结算跳过: itemId={}, status={} (非拍卖中)", itemId, item.getStatus());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 3. 反狙击场景：若当前 endTime 晚于消息预期的 endTime，说明被延长了，暂不结算
            //    延长后的结算会由新的延迟消息处理（若需要，可在反狙击延长时再投递一条）
            if (item.getEndTime() != null && msg.getExpectedEndTimeMs() != null) {
                long actualEndMs = item.getEndTime()
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (actualEndMs > msg.getExpectedEndTimeMs() + 5000) {
                    // endTime 被延长超过 5s，说明发生了反狙击，跳过此次结算
                    log.info("结算延后: itemId={}, actualEnd={}, msgEnd={} (反狙击延长)",
                            itemId, actualEndMs, msg.getExpectedEndTimeMs());
                    channel.basicAck(deliveryTag, false);
                    return;
                }
            }

            // 4. 查询最高有效出价
            BizBid topBid = bidMapper.selectOne(
                    new LambdaQueryWrapper<BizBid>()
                            .eq(BizBid::getItemId, itemId)
                            .eq(BizBid::getStatus, 1) // 有效出价
                            .orderByDesc(BizBid::getBidPrice)
                            .last("LIMIT 1"));

            LocalDateTime now = LocalDateTime.now();

            if (topBid != null) {
                // 5a. 有出价 → 成交
                auctionItemService.lambdaUpdate()
                        .eq(BizAuctionItem::getId, itemId)
                        .set(BizAuctionItem::getStatus, 5)          // 已成交
                        .set(BizAuctionItem::getWinnerId, topBid.getBidderId())
                        .set(BizAuctionItem::getFinalPrice, topBid.getBidPrice())
                        .set(BizAuctionItem::getActualEndTime, now)
                        .update();

                log.info("拍卖结算成交: itemId={}, winnerId={}, finalPrice={}",
                        itemId, topBid.getBidderId(), topBid.getBidPrice());
                orderService.createPendingOrder(item, topBid.getBidderId(), topBid.getId(), topBid.getBidPrice());
                wsPusher.pushAuctionStateChange(itemId, 5,
                        "拍卖结束，成交价：" + topBid.getBidPrice().toPlainString());
                AuctionWonMessage wonMsg = AuctionWonMessage.builder()
                        .itemId(itemId)
                        .itemTitle(item.getTitle())
                        .winnerId(topBid.getBidderId())
                        .finalPrice(topBid.getBidPrice())
                        .bidId(topBid.getId())
                        .build();
                rabbitTemplate.convertAndSend(
                        MqConstants.EXCHANGE_DIRECT,
                        MqConstants.RK_AUCTION_WON,
                        wonMsg);
            } else {
                // 5b. 无出价 → 流拍
                auctionItemService.lambdaUpdate()
                        .eq(BizAuctionItem::getId, itemId)
                        .set(BizAuctionItem::getStatus, 6)          // 流拍
                        .set(BizAuctionItem::getActualEndTime, now)
                        .update();

                log.info("拍卖流拍: itemId={}", itemId);
                wsPusher.pushAuctionStateChange(itemId, 6, "拍卖结束，无人出价，流拍");
            }

            // 6. 清理 Redis 缓存
            redisTemplate.delete(RedisKey.itemPrice(itemId));
            redisTemplate.delete(RedisKey.itemStatus(itemId));

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("结算异常: itemId={}, error={}", itemId, e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
