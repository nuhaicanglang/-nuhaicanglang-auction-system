package com.auction.business.bid.validator;

import com.auction.business.bid.AbstractBidValidator;
import com.auction.business.bid.BidContext;
import com.auction.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 5. 出价金额初步校验：必须 >= 起拍价，且不超过一口价（如设置）。
 * 注意：与「最低加价」的精确比较由 Lua 脚本完成（Redis 当前价 + increment），
 * 这里只做静态字段校验，避免无意义请求穿透到 Redis。
 */
@Component
public class BidPriceValidator extends AbstractBidValidator {

    @Override
    public int order() {
        return 50;
    }

    @Override
    protected void doValidate(BidContext ctx) {
        if (ctx.getItem() == null) {
            return;
        }
        BigDecimal price = ctx.getPrice();
        BigDecimal startPrice = ctx.getItem().getStartPrice();

        if (startPrice != null && price.compareTo(startPrice) < 0) {
            throw new BizException(40001, "出价不能低于起拍价 " + startPrice.toPlainString());
        }

        BigDecimal buyNow = ctx.getItem().getBuyNowPrice();
        if (buyNow != null && price.compareTo(buyNow) > 0) {
            throw new BizException(40006, "出价不能高于一口价 " + buyNow.toPlainString() + "，请使用一口价接口");
        }
    }
}
