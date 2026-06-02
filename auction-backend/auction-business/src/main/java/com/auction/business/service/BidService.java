package com.auction.business.service;

import com.auction.business.vo.BidResultVO;
import com.auction.business.vo.BidVO;
import com.auction.business.vo.MyBidVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;

/**
 * 出价服务接口。
 */
public interface BidService {

    /**
     * 出价。
     * @param itemId    商品ID
     * @param userId    出价人ID
     * @param price     出价金额
     * @param requestId 客户端幂等ID
     * @param clientIp  客户端IP
     * @return 出价结果
     */
    BidResultVO placeBid(Long itemId, Long userId, BigDecimal price, String requestId, String clientIp);

    /**
     * 一口价成交。
     * @param itemId    商品ID
     * @param userId    买家ID
     * @param requestId 客户端幂等ID
     * @param clientIp  客户端IP
     * @return 成交结果（deal=true, status=5）
     */
    BidResultVO buyNow(Long itemId, Long userId, String requestId, String clientIp);

    /**
     * 查询商品的出价记录（分页，按时间倒序）。
     */
    IPage<BidVO> listBids(Long itemId, int page, int size);

    /**
     * 查询当前用户的竞拍流水（分页，按时间倒序）。
     */
    IPage<MyBidVO> listMyBids(Long userId, int page, int size);
}
