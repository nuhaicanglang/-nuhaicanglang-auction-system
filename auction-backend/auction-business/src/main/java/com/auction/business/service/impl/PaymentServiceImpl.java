package com.auction.business.service.impl;

import com.auction.business.dto.WalletAdjustCmd;
import com.auction.business.entity.BizOrder;
import com.auction.business.entity.BizPayment;
import com.auction.business.mapper.BizOrderMapper;
import com.auction.business.mapper.BizPaymentMapper;
import com.auction.business.service.PaymentService;
import com.auction.business.service.WalletService;
import com.auction.business.vo.PaymentVO;
import com.auction.common.core.ErrorCode;
import com.auction.common.exception.BizException;
import com.auction.common.util.SnowflakeIdWorker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务实现。
 * 当前为模拟钱包支付：真实扣款走 WalletService，支付流水用于记录订单支付结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final DateTimeFormatter PAY_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BizOrderMapper orderMapper;
    private final BizPaymentMapper paymentMapper;
    private final WalletService walletService;
    private final RedissonClient redissonClient;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO payOrder(Long orderId, Long userId, String idempotentKey) {
        if (orderId == null || userId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        String idemKey = StringUtils.hasText(idempotentKey)
                ? idempotentKey
                : "PAY_ORDER:" + orderId + ":" + userId;

        BizPayment exists = findByIdempotentKey(idemKey);
        if (exists != null) {
            return toVO(exists);
        }

        RLock lock = redissonClient.getLock("payment:order:" + orderId);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException(99999, "订单支付处理中，请稍后重试");
            }
            exists = findByIdempotentKey(idemKey);
            if (exists != null) {
                return toVO(exists);
            }
            BizPayment paid = paymentMapper.selectOne(new LambdaQueryWrapper<BizPayment>()
                    .eq(BizPayment::getOrderId, orderId)
                    .last("LIMIT 1"));
            if (paid != null) {
                return toVO(paid);
            }

            BizOrder order = orderMapper.selectById(orderId);
            if (order == null) {
                throw new BizException(ErrorCode.ORDER_NOT_FOUND);
            }
            if (!order.getBuyerId().equals(userId)) {
                throw new BizException(50002, "只能支付自己的订单");
            }
            if (!Integer.valueOf(1).equals(order.getStatus())) {
                throw new BizException(50003, "当前订单状态不允许支付");
            }
            LocalDateTime now = LocalDateTime.now();
            if (order.getPayDeadline() != null && now.isAfter(order.getPayDeadline())) {
                throw new BizException(50003, "订单已超过支付截止时间");
            }

            BigDecimal amount = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                WalletAdjustCmd walletCmd = new WalletAdjustCmd();
                walletCmd.setUserId(userId);
                walletCmd.setActionType("DEDUCT");
                walletCmd.setAmount(amount);
                walletCmd.setBizType("ORDER_PAYMENT");
                walletCmd.setBizId(String.valueOf(orderId));
                walletCmd.setRelatedItemId(order.getItemId());
                walletCmd.setOperatorId(userId);
                walletCmd.setRemark("订单支付扣款");
                walletCmd.setIdempotentKey("ORDER_PAY_DEDUCT:" + orderId + ":" + userId);
                walletService.adjust(walletCmd);
            }

            long paymentId = idWorker.nextId();
            BizPayment payment = new BizPayment();
            payment.setId(paymentId);
            payment.setPaymentNo(buildPaymentNo(paymentId, now));
            payment.setOrderId(orderId);
            payment.setOrderNo(order.getOrderNo());
            payment.setPayerId(userId);
            payment.setAmount(amount);
            payment.setPayMethod("WALLET");
            payment.setStatus(1);
            payment.setPaidAt(now);
            payment.setIdempotentKey(idemKey);
            payment.setRemark("模拟钱包支付成功");
            payment.setTenantId(0L);
            payment.setCreatedAt(now);
            payment.setUpdatedAt(now);
            paymentMapper.insert(payment);

            BizOrder update = new BizOrder();
            update.setId(orderId);
            update.setStatus(2);
            update.setPaidAt(now);
            update.setUpdatedAt(now);
            orderMapper.updateById(update);

            log.info("订单支付成功: orderId={}, userId={}, amount={}, paymentNo={}",
                    orderId, userId, amount, payment.getPaymentNo());
            return toVO(payment);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(99999, "订单支付被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private BizPayment findByIdempotentKey(String idempotentKey) {
        return paymentMapper.selectOne(new LambdaQueryWrapper<BizPayment>()
                .eq(BizPayment::getIdempotentKey, idempotentKey)
                .last("LIMIT 1"));
    }

    private String buildPaymentNo(long paymentId, LocalDateTime now) {
        String suffix = String.valueOf(paymentId);
        suffix = suffix.substring(Math.max(0, suffix.length() - 8));
        return "PY" + PAY_NO_TIME.format(now) + suffix;
    }

    private PaymentVO toVO(BizPayment payment) {
        PaymentVO vo = new PaymentVO();
        vo.setId(payment.getId());
        vo.setPaymentNo(payment.getPaymentNo());
        vo.setOrderId(payment.getOrderId());
        vo.setOrderNo(payment.getOrderNo());
        vo.setPayerId(payment.getPayerId());
        vo.setAmount(payment.getAmount());
        vo.setPayMethod(payment.getPayMethod());
        vo.setStatus(payment.getStatus());
        vo.setStatusText(payment.getStatus() != null && payment.getStatus() == 1 ? "成功" : "未知");
        vo.setPaidAt(payment.getPaidAt());
        vo.setCreatedAt(payment.getCreatedAt());
        return vo;
    }
}
