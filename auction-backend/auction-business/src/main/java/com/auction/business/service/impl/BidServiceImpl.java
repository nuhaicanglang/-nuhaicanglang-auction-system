package com.auction.business.service.impl;

import com.auction.business.bid.BidContext;
import com.auction.business.bid.BidValidatorChain;
import com.auction.business.entity.BizAuctionItem;
import com.auction.business.entity.BizBid;
import com.auction.business.mapper.BizBidMapper;
import com.auction.business.service.AuctionItemService;
import com.auction.business.service.BidService;
import com.auction.business.service.OrderService;
import com.auction.business.service.WalletService;
import com.auction.business.vo.BidResultVO;
import com.auction.business.vo.BidVO;
import com.auction.common.exception.BizException;
import com.auction.framework.websocket.WsPusher;
import com.auction.common.util.SnowflakeIdWorker;
import com.auction.framework.redis.RedisKey;
import com.auction.mq.constant.MqConstants;
import com.auction.mq.message.AuctionSettleMessage;
import com.auction.mq.message.AuctionWonMessage;
import com.auction.mq.message.BidMessage;
import com.auction.mq.message.ItemSyncMessage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 出价服务实现。
 * <p>
 * 核心流程（普通出价）：
 * 1. 责任链校验（参数/状态/自我出价/频率/价格）
 * 2. Lua 原子出价（bid.lua）
 * 3. 持久化 MySQL + 更新商品冗余字段
 * 4. 反狙击：若剩余时间 < anti_snipe_min 则延长 end_time
 * 5. 一口价触达：若出价 >= buy_now_price 则直接成交
 * 6. WebSocket 广播
 * </p>
 */
@Slf4j
@Service
public class BidServiceImpl implements BidService {

    private static final long MQ_CONFIRM_TIMEOUT_MS = 5000L;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> bidScript;
    private final DefaultRedisScript<Long> buyNowScript;
    private final AuctionItemService auctionItemService;
    private final BizBidMapper bidMapper;
    private final BidValidatorChain validatorChain;
    private final WsPusher wsPusher;
    private final RabbitTemplate rabbitTemplate;
    private final OrderService orderService;
    private final WalletService walletService;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    public BidServiceImpl(StringRedisTemplate redisTemplate,
                          @Qualifier("bidScript") DefaultRedisScript<Long> bidScript,
                          @Qualifier("buyNowScript") DefaultRedisScript<Long> buyNowScript,
                          AuctionItemService auctionItemService,
                          BizBidMapper bidMapper,
                          BidValidatorChain validatorChain,
                          WsPusher wsPusher,
                          RabbitTemplate rabbitTemplate,
                          OrderService orderService,
                          WalletService walletService) {
        this.redisTemplate = redisTemplate;
        this.bidScript = bidScript;
        this.buyNowScript = buyNowScript;
        this.auctionItemService = auctionItemService;
        this.bidMapper = bidMapper;
        this.validatorChain = validatorChain;
        this.wsPusher = wsPusher;
        this.rabbitTemplate = rabbitTemplate;
        this.orderService = orderService;
        this.walletService = walletService;
    }

    @Override
    public BidResultVO placeBid(Long itemId, Long userId, BigDecimal price,
                                String requestId, String clientIp) {
        // 1. 责任链校验：参数 → 商品状态 → 不能给自己出价 → 频率 → 价格
        //    任一节点失败即抛 BizException 中断流程。商品对象由链中加载并写入 ctx。
        BidContext ctx = BidContext.builder()
                .itemId(itemId)
                .userId(userId)
                .price(price)
                .requestId(requestId)
                .clientIp(clientIp)
                .build();
        validatorChain.execute(ctx);
        BizAuctionItem item = ctx.getItem();
        BigDecimal deposit = item.getDeposit() == null ? BigDecimal.ZERO : item.getDeposit();
        boolean frozenThisTime = walletService.freezeBidDeposit(userId, itemId, deposit, requestId);

        // 2. 执行 Lua 脚本原子出价
        long bidId = idWorker.nextId();
        long bidTimeMs = System.currentTimeMillis();

        Long result;
        try {
            result = redisTemplate.execute(bidScript,
                    List.of(
                            RedisKey.itemPrice(itemId),
                            RedisKey.bidQueue(itemId),
                            RedisKey.idem(requestId),
                            RedisKey.itemStatus(itemId)
                    ),
                    String.valueOf(userId),
                    price.toPlainString(),
                    String.valueOf(bidTimeMs),
                    item.getBidIncrement().toPlainString(),
                    String.valueOf(bidId),
                    requestId,
                    item.getBuyNowPrice() != null ? item.getBuyNowPrice().toPlainString() : "",
                    "5"
            );
        } catch (RuntimeException e) {
            rollbackDepositIfNeeded(userId, itemId, deposit, requestId, frozenThisTime);
            throw e;
        }

        if (result == null) {
            rollbackDepositIfNeeded(userId, itemId, deposit, requestId, frozenThisTime);
            throw new BizException(99999, "出价脚本执行异常");
        }
        if (result == 0) {
            rollbackDepositIfNeeded(userId, itemId, deposit, requestId, frozenThisTime);
            // 读取 Redis 当前价，提示最低出价
            String curStr = redisTemplate.opsForValue().get(RedisKey.itemPrice(itemId));
            BigDecimal cur = curStr != null ? new BigDecimal(curStr) : item.getCurrentPrice();
            BigDecimal minBid = cur.add(item.getBidIncrement());
            throw new BizException(40001, "出价金额不足，最低出价 " + minBid.toPlainString());
        }
        if (result == -1) {
            rollbackDepositIfNeeded(userId, itemId, deposit, requestId, frozenThisTime);
            throw new BizException(40004, "重复请求，请勿重复提交");
        }
        if (result == -2) {
            rollbackDepositIfNeeded(userId, itemId, deposit, requestId, frozenThisTime);
            throw new BizException(40003, "拍卖已成交，无法继续出价");
        }

        // 3. 异步持久化：发送 MQ 消息，由 BidPersistConsumer 落库
        BidMessage bidMsg = BidMessage.builder()
                .bidId(bidId)
                .itemId(itemId)
                .bidderId(userId)
                .bidPrice(price)
                .bidTimeMs(bidTimeMs)
                .bidType(1)
                .clientIp(clientIp)
                .clientRequestId(requestId)
                .build();
        publishBidPersistMessage(bidMsg, requestId);

        log.info("出价成功: itemId={}, userId={}, price={}, bidId={}", itemId, userId, price, bidId);

        // 4. 反狙击：若开启且剩余时间 < anti_snipe_min，延长 end_time
        BidResultVO vo = new BidResultVO();
        vo.setBidId(bidId);
        vo.setCurrentPrice(price);
        vo.setDeal(false);
        vo.setExtended(false);
        vo.setStatus(3);

        if (result != 2 && item.getIsAntiSnipe() != null && item.getIsAntiSnipe() == 1
                && item.getEndTime() != null && item.getAntiSnipeMin() != null) {
            long remainSec = java.time.Duration.between(LocalDateTime.now(), item.getEndTime()).getSeconds();
            if (remainSec > 0 && remainSec < item.getAntiSnipeMin() * 60L) {
                // 延长：将结束时间推迟到 now + anti_snipe_min
                LocalDateTime newEnd = LocalDateTime.now().plusMinutes(item.getAntiSnipeMin());
                auctionItemService.lambdaUpdate()
                        .eq(BizAuctionItem::getId, itemId)
                        .set(BizAuctionItem::getEndTime, newEnd)
                        .set(BizAuctionItem::getActualEndTime, newEnd)
                        .update();
                vo.setExtended(true);
                vo.setEndTime(newEnd);
                log.info("反狙击延时: itemId={}, newEnd={}", itemId, newEnd);
                // 广播状态变化（仍为拍卖中，但 endTime 变了）
                wsPusher.pushAuctionStateChange(itemId, 3,
                        "反狙击延时，新结束时间：" + newEnd);
                // 重新投递延迟结算消息（旧消息会被 AuctionSettleConsumer 的时间校验跳过）
                long ttlMs = java.time.Duration.between(LocalDateTime.now(), newEnd).toMillis();
                if (ttlMs < 1000) ttlMs = 1000;
                AuctionSettleMessage settleMsg = AuctionSettleMessage.builder()
                        .itemId(itemId)
                        .expectedEndTimeMs(newEnd.atZone(java.time.ZoneId.systemDefault())
                                .toInstant().toEpochMilli())
                        .build();
                String exp = String.valueOf(ttlMs);
                rabbitTemplate.convertAndSend(
                        MqConstants.EXCHANGE_DIRECT,
                        MqConstants.RK_AUCTION_DELAY,
                        settleMsg,
                        m -> { m.getMessageProperties().setExpiration(exp); return m; });
            }
        }

        // 5. 一口价触达：出价 >= buy_now_price → 直接成交
        if (result == 2) {
            auctionItemService.lambdaUpdate()
                    .eq(BizAuctionItem::getId, itemId)
                    .set(BizAuctionItem::getStatus, 5)   // 5=已成交
                    .set(BizAuctionItem::getWinnerId, userId)
                    .set(BizAuctionItem::getFinalPrice, price)
                    .set(BizAuctionItem::getActualEndTime, LocalDateTime.now())
                    .update();
            vo.setDeal(true);
            vo.setStatus(5);
            log.info("一口价触达成交: itemId={}, winnerId={}, price={}", itemId, userId, price);
            orderService.createPendingOrder(item, userId, bidId, price);
            walletService.settleBidDeposits(itemId, userId, deposit);
            wsPusher.pushAuctionStateChange(itemId, 5, "一口价成交，成交价：" + price.toPlainString());
            AuctionWonMessage wonMsg = AuctionWonMessage.builder()
                    .itemId(itemId)
                    .itemTitle(item.getTitle())
                    .winnerId(userId)
                    .finalPrice(price)
                    .bidId(bidId)
                    .build();
            rabbitTemplate.convertAndSend(
                    MqConstants.EXCHANGE_DIRECT,
                    MqConstants.RK_AUCTION_WON,
                    wonMsg);
            // 同步 ES 索引（一口价成交，状态=5）
            rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_DIRECT, MqConstants.RK_ITEM_SYNC,
                    ItemSyncMessage.builder().itemId(itemId).action("UPSERT").build());
        }

        // 6. WebSocket 出价广播
        String idStr = String.valueOf(userId);
        String bidderName = idStr.charAt(0) + "***" + idStr.charAt(idStr.length() - 1);
        wsPusher.pushBidPlaced(itemId, userId, bidderName, price, bidId);

        return vo;
    }

    @Override
    public IPage<BidVO> listBids(Long itemId, int page, int size) {
        Page<BizBid> p = new Page<>(page, size);
        LambdaQueryWrapper<BizBid> wrapper = new LambdaQueryWrapper<BizBid>()
                .eq(BizBid::getItemId, itemId)
                .orderByDesc(BizBid::getBidTime);
        Page<BizBid> result = bidMapper.selectPage(p, wrapper);
        return result.convert(this::toVO);
    }

    // ----------------------------------------------------------------
    // 一口价
    // ----------------------------------------------------------------

    @Override
    public BidResultVO buyNow(Long itemId, Long userId, String requestId, String clientIp) {
        // 1. 加载商品，基础校验
        BizAuctionItem item = auctionItemService.getById(itemId);
        if (item == null) {
            throw new BizException(30003, "商品不存在");
        }
        if (item.getStatus() == null || item.getStatus() != 3) {
            throw new BizException(40003, "拍卖未开始或已结束");
        }
        if (item.getSellerId().equals(userId)) {
            throw new BizException(40002, "不能购买自己的商品");
        }
        if (item.getBuyNowPrice() == null) {
            throw new BizException(40006, "该商品未设置一口价");
        }
        LocalDateTime now = LocalDateTime.now();
        if (item.getEndTime() != null && now.isAfter(item.getEndTime())) {
            throw new BizException(40003, "拍卖已结束");
        }

        BigDecimal buyNowPrice = item.getBuyNowPrice();
        BigDecimal deposit = item.getDeposit() == null ? BigDecimal.ZERO : item.getDeposit();
        boolean frozenThisTime = walletService.freezeBidDeposit(userId, itemId, deposit, requestId);
        long bidId = idWorker.nextId();
        long bidTimeMs = System.currentTimeMillis();

        // 2. 执行 buy_now Lua 脚本：原子写价格 + Redis 状态 key + 幂等
        //    KEYS: [price, queue, idem, status]
        //    ARGV: [userId, buyNowPrice, bidTimeMs, bidId, requestId, statusValue("5")]
        Long result;
        try {
            result = redisTemplate.execute(buyNowScript,
                    List.of(
                            RedisKey.itemPrice(itemId),
                            RedisKey.bidQueue(itemId),
                            RedisKey.idem(requestId),
                            RedisKey.itemStatus(itemId)
                    ),
                    String.valueOf(userId),
                    buyNowPrice.toPlainString(),
                    String.valueOf(bidTimeMs),
                    String.valueOf(bidId),
                    requestId,
                    "5"
            );
        } catch (RuntimeException e) {
            rollbackDepositIfNeeded(userId, itemId, deposit, requestId, frozenThisTime);
            throw e;
        }

        if (result == null) {
            rollbackDepositIfNeeded(userId, itemId, deposit, requestId, frozenThisTime);
            throw new BizException(99999, "一口价脚本执行异常");
        }
        if (result == -1) {
            rollbackDepositIfNeeded(userId, itemId, deposit, requestId, frozenThisTime);
            throw new BizException(40004, "重复请求，请勿重复提交");
        }
        if (result == 0) {
            rollbackDepositIfNeeded(userId, itemId, deposit, requestId, frozenThisTime);
            throw new BizException(40003, "拍卖已结束，无法一口价购买");
        }

        // 3. 异步持久化出价记录
        BidMessage bidMsg = BidMessage.builder()
                .bidId(bidId)
                .itemId(itemId)
                .bidderId(userId)
                .bidPrice(buyNowPrice)
                .bidTimeMs(bidTimeMs)
                .bidType(3)
                .clientIp(clientIp)
                .clientRequestId(requestId)
                .build();
        publishBidPersistMessage(bidMsg, requestId);

        // 4. 同步更新商品状态为已成交（一口价需立即生效）
        auctionItemService.lambdaUpdate()
                .eq(BizAuctionItem::getId, itemId)
                .set(BizAuctionItem::getStatus, 5)
                .set(BizAuctionItem::getCurrentPrice, buyNowPrice)
                .set(BizAuctionItem::getWinnerId, userId)
                .set(BizAuctionItem::getFinalPrice, buyNowPrice)
                .set(BizAuctionItem::getActualEndTime, LocalDateTime.now())
                .update();

        log.info("一口价成交: itemId={}, winnerId={}, price={}, bidId={}", itemId, userId, buyNowPrice, bidId);
        orderService.createPendingOrder(item, userId, bidId, buyNowPrice);
        walletService.settleBidDeposits(itemId, userId, deposit);

        // 5. WebSocket 广播
        String idStr = String.valueOf(userId);
        String bidderName = idStr.charAt(0) + "***" + idStr.charAt(idStr.length() - 1);
        wsPusher.pushBidPlaced(itemId, userId, bidderName, buyNowPrice, bidId);
        wsPusher.pushAuctionStateChange(itemId, 5, "一口价成交，成交价：" + buyNowPrice.toPlainString());
        AuctionWonMessage wonMsg = AuctionWonMessage.builder()
                .itemId(itemId)
                .itemTitle(item.getTitle())
                .winnerId(userId)
                .finalPrice(buyNowPrice)
                .bidId(bidId)
                .build();
        rabbitTemplate.convertAndSend(
                MqConstants.EXCHANGE_DIRECT,
                MqConstants.RK_AUCTION_WON,
                wonMsg);
        // 同步 ES 索引（一口价成交）
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_DIRECT, MqConstants.RK_ITEM_SYNC,
                ItemSyncMessage.builder().itemId(itemId).action("UPSERT").build());

        BidResultVO vo = new BidResultVO();
        vo.setBidId(bidId);
        vo.setCurrentPrice(buyNowPrice);
        vo.setDeal(true);
        vo.setExtended(false);
        vo.setStatus(5);
        return vo;
    }

    private void rollbackDepositIfNeeded(Long userId, Long itemId, BigDecimal deposit,
                                         String requestId, boolean frozenThisTime) {
        if (frozenThisTime) {
            walletService.cancelBidDepositFreeze(userId, itemId, deposit, requestId);
        }
    }

    private void publishBidPersistMessage(BidMessage bidMsg, String requestId) {
        try {
            rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(
                        MqConstants.EXCHANGE_DIRECT,
                        MqConstants.RK_BID_PERSIST,
                        bidMsg,
                        new CorrelationData(requestId));
                operations.waitForConfirmsOrDie(MQ_CONFIRM_TIMEOUT_MS);
                return null;
            });
        } catch (AmqpException e) {
            log.error("出价消息发布失败，降级同步落库: itemId={}, bidId={}, requestId={}",
                    bidMsg.getItemId(), bidMsg.getBidId(), requestId, e);
            persistBidFallback(bidMsg);
        }
    }

    private void persistBidFallback(BidMessage msg) {
        BizBid bid = new BizBid();
        bid.setId(msg.getBidId());
        bid.setItemId(msg.getItemId());
        bid.setBidderId(msg.getBidderId());
        bid.setBidPrice(msg.getBidPrice());
        bid.setBidTime(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(msg.getBidTimeMs()), ZoneId.systemDefault()));
        bid.setBidType(msg.getBidType());
        bid.setStatus(1);
        bid.setClientIp(msg.getClientIp());
        bid.setClientRequestId(msg.getClientRequestId());
        bid.setTenantId(0L);
        try {
            bidMapper.insert(bid);
        } catch (DuplicateKeyException e) {
            log.info("出价降级落库命中幂等记录: requestId={}", msg.getClientRequestId());
            return;
        }
        auctionItemService.lambdaUpdate()
                .eq(BizAuctionItem::getId, msg.getItemId())
                .set(BizAuctionItem::getCurrentPrice, msg.getBidPrice())
                .setSql("bid_count = bid_count + 1")
                .update();
    }

    private BidVO toVO(BizBid bid) {
        BidVO vo = new BidVO();
        vo.setId(bid.getId());
        vo.setItemId(bid.getItemId());
        vo.setBidderId(bid.getBidderId());
        // 脱敏：只显示首尾字符
        String idStr = String.valueOf(bid.getBidderId());
        vo.setBidderName(idStr.charAt(0) + "***" + idStr.charAt(idStr.length() - 1));
        vo.setBidPrice(bid.getBidPrice());
        vo.setBidTime(bid.getBidTime());
        vo.setBidType(bid.getBidType());
        vo.setStatus(bid.getStatus());
        return vo;
    }
}
