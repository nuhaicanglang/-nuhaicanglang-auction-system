package com.auction.business.bid.validator;

import com.auction.business.bid.AbstractBidValidator;
import com.auction.business.bid.BidContext;
import com.auction.common.exception.BizException;
import org.springframework.stereotype.Component;

/**
 * 3. 不能给自己的商品出价。
 * 依赖 ItemStatusValidator 已经把 item 写入 ctx。
 */
@Component
public class SelfBidValidator extends AbstractBidValidator {

    @Override
    public int order() {
        return 30;
    }

    @Override
    protected void doValidate(BidContext ctx) {
        if (ctx.getItem() == null) {
            return; // 防御：上一节点未加载到 item（理论上不会，因为它会抛异常中断）
        }
        if (ctx.getItem().getSellerId().equals(ctx.getUserId())) {
            throw new BizException(40002, "不能给自己的商品出价");
        }
    }
}
