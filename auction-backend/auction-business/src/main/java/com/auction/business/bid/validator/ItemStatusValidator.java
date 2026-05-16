package com.auction.business.bid.validator;

import com.auction.business.bid.AbstractBidValidator;
import com.auction.business.bid.BidContext;
import com.auction.business.entity.BizAuctionItem;
import com.auction.business.service.AuctionItemService;
import com.auction.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 2. 商品状态校验器。
 * 加载商品信息并检查必须为「拍卖中」(status=3) 且时间窗口有效。
 * 加载到的 BizAuctionItem 写入 ctx，供后续校验器复用，避免多次查库。
 */
@Component
@RequiredArgsConstructor
public class ItemStatusValidator extends AbstractBidValidator {

    private final AuctionItemService auctionItemService;

    @Override
    public int order() {
        return 20;
    }

    @Override
    protected void doValidate(BidContext ctx) {
        BizAuctionItem item = auctionItemService.getById(ctx.getItemId());
        if (item == null || item.getDeleted() != null && item.getDeleted() != 0) {
            throw new BizException(30003, "商品不存在");
        }
        if (item.getStatus() == null || item.getStatus() != 3) {
            throw new BizException(40003, "拍卖未开始或已结束");
        }
        LocalDateTime now = LocalDateTime.now();
        if (item.getEndTime() != null && now.isAfter(item.getEndTime())) {
            throw new BizException(40003, "拍卖已结束");
        }
        if (item.getStartTime() != null && now.isBefore(item.getStartTime())) {
            throw new BizException(40003, "拍卖尚未开始");
        }
        // 写入上下文，后续校验器与主流程共享
        ctx.setItem(item);
    }
}
