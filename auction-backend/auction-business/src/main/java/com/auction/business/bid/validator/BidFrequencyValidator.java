package com.auction.business.bid.validator;

import com.auction.business.bid.AbstractBidValidator;
import com.auction.business.bid.BidContext;
import com.auction.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 4. 出价频率限制：同一用户对同一商品 1 秒内只能出 1 次价。
 * 用 Redis SETNX + EXPIRE 实现。
 */
@Component
@RequiredArgsConstructor
public class BidFrequencyValidator extends AbstractBidValidator {

    private final StringRedisTemplate redisTemplate;

    /** 频率窗口（秒） */
    private static final long WINDOW_SECONDS = 1;

    @Override
    public int order() {
        return 40;
    }

    @Override
    protected void doValidate(BidContext ctx) {
        String key = "bid:rate:" + ctx.getUserId() + ":" + ctx.getItemId();
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(WINDOW_SECONDS));
        if (Boolean.FALSE.equals(ok)) {
            throw new BizException(40005, "出价过于频繁，请稍候再试");
        }
    }
}
