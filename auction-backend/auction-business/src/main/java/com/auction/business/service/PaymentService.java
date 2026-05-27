package com.auction.business.service;

import com.auction.business.vo.PaymentVO;

/**
 * 支付服务接口。
 */
public interface PaymentService {

    PaymentVO payOrder(Long orderId, Long userId, String idempotentKey);
}
