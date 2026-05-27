package com.auction.business.consumer;

import com.auction.business.entity.BizAuctionItem;
import com.auction.business.mapper.BizAuctionItemMapper;
import com.auction.mq.constant.MqConstants;
import com.auction.mq.message.ItemSyncMessage;
import com.auction.search.doc.ItemDoc;
import com.auction.search.repository.ItemDocRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * ES 商品同步消费者。
 * 监听 item.sync.queue，根据消息内容对 ES 索引执行 UPSERT 或 DELETE。
 * 增量同步：每当商品发生变更（创建、编辑、状态变化），生产者投递消息到此队列。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsSyncConsumer {

    private final BizAuctionItemMapper itemMapper;
    private final ItemDocRepository itemDocRepository;

    @RabbitListener(queues = MqConstants.QUEUE_ITEM_SYNC, ackMode = "MANUAL")
    public void onMessage(ItemSyncMessage msg, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            if (msg == null || msg.getItemId() == null) {
                channel.basicAck(tag, false);
                return;
            }
            if ("DELETE".equals(msg.getAction())) {
                itemDocRepository.deleteById(msg.getItemId());
                log.info("ES 删除文档: itemId={}", msg.getItemId());
            } else {
                BizAuctionItem item = itemMapper.selectById(msg.getItemId());
                if (item == null || item.getDeleted() != null && item.getDeleted() == 1) {
                    itemDocRepository.deleteById(msg.getItemId());
                    log.info("ES 删除文档(商品不存在或已删除): itemId={}", msg.getItemId());
                } else {
                    ItemDoc doc = toDoc(item);
                    itemDocRepository.save(doc);
                    log.debug("ES 同步文档: itemId={}, status={}", item.getId(), item.getStatus());
                }
            }
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("ES 同步失败: msg={}", msg, e);
            channel.basicNack(tag, false, true);
        }
    }

    /** 实体转 ES 文档 */
    public static ItemDoc toDoc(BizAuctionItem item) {
        ItemDoc doc = new ItemDoc();
        doc.setId(item.getId());
        doc.setTitle(item.getTitle());
        doc.setSubtitle(item.getSubtitle());
        doc.setCategoryId(item.getCategoryId());
        doc.setCategoryPath(item.getCategoryPath());
        doc.setCoverImage(item.getCoverImage());
        doc.setSellerId(item.getSellerId());
        doc.setStartPrice(item.getStartPrice());
        doc.setCurrentPrice(item.getCurrentPrice());
        doc.setBuyNowPrice(item.getBuyNowPrice());
        doc.setBidCount(item.getBidCount());
        doc.setViewCount(item.getViewCount());
        doc.setStatus(item.getStatus());
        doc.setStartTime(item.getStartTime());
        doc.setEndTime(item.getEndTime());
        doc.setCreatedAt(item.getCreatedAt());
        return doc;
    }
}
