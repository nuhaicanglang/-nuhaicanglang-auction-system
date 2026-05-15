package com.auction.business.runner;

import com.auction.business.entity.BizAuctionItem;
import com.auction.business.mapper.BizAuctionItemMapper;
import com.auction.framework.redis.RedisKey;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用启动时预热 Redis：
 * 将所有「拍卖中」(status=3) 商品的当前价加载到 Redis。
 * 这样出价 Lua 脚本能直接读取 Redis 中的价格进行原子比较。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionCacheWarmUp implements ApplicationRunner {

    private final BizAuctionItemMapper itemMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<BizAuctionItem> activeItems = itemMapper.selectList(
                new LambdaQueryWrapper<BizAuctionItem>()
                        .eq(BizAuctionItem::getStatus, 3)
                        .eq(BizAuctionItem::getDeleted, 0)
        );

        for (BizAuctionItem item : activeItems) {
            String key = RedisKey.itemPrice(item.getId());
            redisTemplate.opsForValue().set(key, item.getCurrentPrice().toPlainString());
        }
        log.info("Redis 预热完成，加载了 {} 件拍卖中商品的价格", activeItems.size());
    }
}
