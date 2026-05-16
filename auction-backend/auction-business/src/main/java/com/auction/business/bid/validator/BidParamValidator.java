package com.auction.business.bid.validator;

import com.auction.business.bid.AbstractBidValidator;
import com.auction.business.bid.BidContext;
import com.auction.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 1. 出价参数校验器：itemId/userId/price 不可为空，价格必须 > 0。
 * 这是最基础的入参检查，放在最前面。
 */
@Component
public class BidParamValidator extends AbstractBidValidator {

    @Override
    public int order() {
        return 10;
    }

    @Override
    protected void doValidate(BidContext ctx) {
        if (ctx.getItemId() == null || ctx.getUserId() == null) {
            throw new BizException(10001, "缺少必要参数");
        }
        if (ctx.getPrice() == null || ctx.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(10001, "出价金额必须大于 0");
        }
        if (ctx.getRequestId() == null || ctx.getRequestId().isBlank()) {
            throw new BizException(10001, "缺少幂等ID");
        }
    }
}
