package com.auction.business.service;

import com.auction.business.dto.OrderQueryDTO;
import com.auction.business.entity.BizAuctionItem;
import com.auction.business.vo.OrderVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;

/**
 * 订单服务接口。
 */
public interface OrderService {

    /** 拍卖成交后创建待支付订单，按 itemId 幂等。 */
    Long createPendingOrder(BizAuctionItem item, Long buyerId, Long bidId, BigDecimal dealPrice);

    /** 买家视角订单列表。 */
    IPage<OrderVO> listBuyerOrders(Long buyerId, OrderQueryDTO query);

    /** 卖家视角订单列表。 */
    IPage<OrderVO> listSellerOrders(Long sellerId, OrderQueryDTO query);

    /** 订单详情，仅买家或卖家可查看。 */
    OrderVO getOrderDetail(Long orderId, Long userId);

    /** 支付超时关闭订单，仅待支付订单会被关闭。 */
    boolean closeTimeoutOrder(Long orderId, Long expectedPayDeadlineMs);
}
