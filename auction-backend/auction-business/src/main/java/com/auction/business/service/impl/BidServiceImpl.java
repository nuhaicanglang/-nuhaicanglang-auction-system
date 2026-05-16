package com.auction.business.service.impl;

import com.auction.business.bid.BidContext;
import com.auction.business.bid.BidValidatorChain;
import com.auction.business.entity.BizAuctionItem;
import com.auction.business.entity.BizBid;
import com.auction.business.mapper.BizBidMapper;
import com.auction.business.service.AuctionItemService;
import com.auction.business.service.BidService;
import com.auction.business.vo.BidResultVO;
import com.auction.business.vo.BidVO;
import com.auction.common.exception.BizException;
import com.auction.common.util.SnowflakeIdWorker;
import com.auction.framework.redis.RedisKey;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 出价服务实现。
 * <p>
 * 核心流程：
 * 1. 校验商品状态（必须为「拍卖中」status=3）
 * 2. 校验不能给自己的商品出价
 * 3. 调用 Redis Lua 脚本原子出价（bid.lua）
 *    - 返回  1：成功
 *    - 返回  0：出价金额不足
 *    - 返回 -1：重复请求（幂等拦截）
 * 4. 成功后同步写入 MySQL（后续改为 MQ 异步持久化）
 * 5. 更新商品冗余字段（current_price, bid_count）
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BidServiceImpl implements BidService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> bidScript;
    private final AuctionItemService auctionItemService;
    private final BizBidMapper bidMapper;
    private final BidValidatorChain validatorChain;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

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

        // 2. 执行 Lua 脚本原子出价
        long bidId = idWorker.nextId();
        long bidTimeMs = System.currentTimeMillis();

        Long result = redisTemplate.execute(bidScript,
                List.of(
                        RedisKey.itemPrice(itemId),
                        RedisKey.bidQueue(itemId),
                        RedisKey.idem(requestId)
                ),
                String.valueOf(userId),
                price.toPlainString(),
                String.valueOf(bidTimeMs),
                item.getBidIncrement().toPlainString(),
                String.valueOf(bidId),
                requestId
        );

        if (result == null) {
            throw new BizException(99999, "出价脚本执行异常");
        }
        if (result == 0) {
            // 读取 Redis 当前价，提示最低出价
            String curStr = redisTemplate.opsForValue().get(RedisKey.itemPrice(itemId));
            BigDecimal cur = curStr != null ? new BigDecimal(curStr) : item.getCurrentPrice();
            BigDecimal minBid = cur.add(item.getBidIncrement());
            throw new BizException(40001, "出价金额不足，最低出价 " + minBid.toPlainString());
        }
        if (result == -1) {
            throw new BizException(40004, "重复请求，请勿重复提交");
        }

        // 3. 同步持久化到 MySQL（后续可改 MQ 异步）
        BizBid bid = new BizBid();
        bid.setId(bidId);
        bid.setItemId(itemId);
        bid.setBidderId(userId);
        bid.setBidPrice(price);
        bid.setBidTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(bidTimeMs), ZoneId.systemDefault()));
        bid.setBidType(1);
        bid.setStatus(1);
        bid.setClientIp(clientIp);
        bid.setClientRequestId(requestId);
        bid.setTenantId(0L);
        bidMapper.insert(bid);

        // 4. 更新商品冗余字段
        auctionItemService.lambdaUpdate()
                .eq(BizAuctionItem::getId, itemId)
                .set(BizAuctionItem::getCurrentPrice, price)
                .setSql("bid_count = bid_count + 1")
                .update();

        log.info("出价成功: itemId={}, userId={}, price={}, bidId={}", itemId, userId, price, bidId);
        return new BidResultVO(bidId, price);
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
