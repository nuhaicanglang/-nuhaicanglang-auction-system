package com.auction.business.service.impl;

import com.auction.business.dto.OrderQueryDTO;
import com.auction.business.entity.BizAuctionItem;
import com.auction.business.entity.BizOrder;
import com.auction.business.mapper.BizOrderMapper;
import com.auction.business.service.OrderService;
import com.auction.business.vo.OrderVO;
import com.auction.common.exception.BizException;
import com.auction.common.util.SnowflakeIdWorker;
import com.auction.mq.constant.MqConstants;
import com.auction.mq.message.CreditEventMessage;
import com.auction.mq.message.OrderTimeoutMessage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 订单服务实现。
 * Day 20 先生成待支付订单；Day 21 钱包和保证金会继续接入这里。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Map<Integer, String> STATUS_MAP = Map.of(
            1, "待支付", 2, "已支付", 3, "已发货", 4, "已完成", 5, "已取消", 6, "已关闭");

    private final BizOrderMapper orderMapper;
    private final RabbitTemplate rabbitTemplate;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPendingOrder(BizAuctionItem item, Long buyerId, Long bidId, BigDecimal dealPrice) {
        BizOrder exists = orderMapper.selectOne(new LambdaQueryWrapper<BizOrder>()
                .eq(BizOrder::getItemId, item.getId())
                .last("LIMIT 1"));
        if (exists != null) {
            return exists.getId();
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal deposit = item.getDeposit() == null ? BigDecimal.ZERO : item.getDeposit();
        BigDecimal payAmount = dealPrice.subtract(deposit);
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
            payAmount = BigDecimal.ZERO;
        }

        long orderId = idWorker.nextId();
        BizOrder order = new BizOrder();
        order.setId(orderId);
        order.setOrderNo(buildOrderNo(orderId, now));
        order.setItemId(item.getId());
        order.setItemTitle(item.getTitle());
        order.setItemCoverImage(item.getCoverImage());
        order.setBuyerId(buyerId);
        order.setSellerId(item.getSellerId());
        order.setBidId(bidId);
        order.setDealPrice(dealPrice);
        order.setDepositAmount(deposit);
        order.setPayAmount(payAmount);
        order.setStatus(1);
        order.setPayDeadline(now.plusMinutes(30));
        order.setTenantId(0L);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            BizOrder duplicated = orderMapper.selectOne(new LambdaQueryWrapper<BizOrder>()
                    .eq(BizOrder::getItemId, item.getId())
                    .last("LIMIT 1"));
            if (duplicated != null) {
                return duplicated.getId();
            }
            throw e;
        }

        log.info("成交订单创建成功: orderId={}, itemId={}, buyerId={}, payAmount={}",
                orderId, item.getId(), buyerId, payAmount);
        sendOrderTimeoutMessage(order);
        return orderId;
    }

    @Override
    public IPage<OrderVO> listBuyerOrders(Long buyerId, OrderQueryDTO query) {
        LambdaQueryWrapper<BizOrder> wrapper = baseQuery(query)
                .eq(BizOrder::getBuyerId, buyerId)
                .orderByDesc(BizOrder::getCreatedAt);
        return pageOrders(query, wrapper);
    }

    @Override
    public IPage<OrderVO> listSellerOrders(Long sellerId, OrderQueryDTO query) {
        LambdaQueryWrapper<BizOrder> wrapper = baseQuery(query)
                .eq(BizOrder::getSellerId, sellerId)
                .orderByDesc(BizOrder::getCreatedAt);
        return pageOrders(query, wrapper);
    }

    @Override
    public IPage<OrderVO> listAdminOrders(OrderQueryDTO query) {
        LambdaQueryWrapper<BizOrder> wrapper = baseQuery(query)
                .orderByDesc(BizOrder::getCreatedAt);
        return pageOrders(query, wrapper);
    }

    @Override
    public OrderVO getOrderDetail(Long orderId, Long userId) {
        BizOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(50001, "订单不存在");
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BizException(50002, "无权查看该订单");
        }
        return toVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeTimeoutOrder(Long orderId, Long expectedPayDeadlineMs) {
        BizOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("订单超时关闭跳过: 订单不存在 orderId={}", orderId);
            return false;
        }
        if (!Integer.valueOf(1).equals(order.getStatus())) {
            log.info("订单超时关闭跳过: orderId={}, status={}", orderId, order.getStatus());
            return false;
        }
        if (order.getPayDeadline() != null && expectedPayDeadlineMs != null) {
            long actualDeadlineMs = order.getPayDeadline()
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (actualDeadlineMs > expectedPayDeadlineMs + 5000) {
                log.info("订单超时关闭跳过: orderId={}, actualDeadline={}, msgDeadline={}",
                        orderId, actualDeadlineMs, expectedPayDeadlineMs);
                return false;
            }
        }
        if (order.getPayDeadline() != null && LocalDateTime.now().isBefore(order.getPayDeadline())) {
            log.info("订单超时关闭跳过: orderId={} 未到支付截止时间", orderId);
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        BizOrder update = new BizOrder();
        update.setId(orderId);
        update.setStatus(6);
        update.setClosedAt(now);
        update.setCloseReason("支付超时自动关闭");
        update.setUpdatedAt(now);
        orderMapper.updateById(update);
        sendCreditEvent("ORDER_TIMEOUT", order.getBuyerId(), orderId);
        log.info("订单支付超时自动关闭: orderId={}, orderNo={}", orderId, order.getOrderNo());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, Long userId) {
        BizOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(50001, "订单不存在");
        }
        if (!order.getSellerId().equals(userId)) {
            throw new BizException(50002, "只有卖家可以发货");
        }
        if (!Integer.valueOf(2).equals(order.getStatus())) {
            throw new BizException(50003, "当前订单状态不允许发货");
        }
        LocalDateTime now = LocalDateTime.now();
        BizOrder update = new BizOrder();
        update.setId(orderId);
        update.setStatus(3);
        update.setShippedAt(now);
        update.setUpdatedAt(now);
        orderMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long orderId, Long userId) {
        BizOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(50001, "订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new BizException(50002, "只有买家可以确认完成订单");
        }
        if (!Integer.valueOf(2).equals(order.getStatus()) && !Integer.valueOf(3).equals(order.getStatus())) {
            throw new BizException(50003, "当前订单状态不允许确认完成");
        }
        LocalDateTime now = LocalDateTime.now();
        BizOrder update = new BizOrder();
        update.setId(orderId);
        update.setStatus(4);
        update.setCompletedAt(now);
        update.setUpdatedAt(now);
        orderMapper.updateById(update);
        sendCreditEvent("ORDER_DONE", order.getBuyerId(), orderId);
        sendCreditEvent("ORDER_DONE", order.getSellerId(), orderId);
    }

    private LambdaQueryWrapper<BizOrder> baseQuery(OrderQueryDTO query) {
        LambdaQueryWrapper<BizOrder> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null) {
            wrapper.eq(BizOrder::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getOrderNo())) {
            wrapper.like(BizOrder::getOrderNo, query.getOrderNo().trim());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(BizOrder::getItemTitle, query.getKeyword().trim());
        }
        if (query.getBuyerId() != null) {
            wrapper.eq(BizOrder::getBuyerId, query.getBuyerId());
        }
        if (query.getSellerId() != null) {
            wrapper.eq(BizOrder::getSellerId, query.getSellerId());
        }
        return wrapper;
    }

    private IPage<OrderVO> pageOrders(OrderQueryDTO query, LambdaQueryWrapper<BizOrder> wrapper) {
        Page<BizOrder> page = new Page<>(query.getPage(), query.getSize());
        Page<BizOrder> result = orderMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    private OrderVO toVO(BizOrder order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setItemId(order.getItemId());
        vo.setItemTitle(order.getItemTitle());
        vo.setItemCoverImage(order.getItemCoverImage());
        vo.setBuyerId(order.getBuyerId());
        vo.setSellerId(order.getSellerId());
        vo.setBidId(order.getBidId());
        vo.setDealPrice(order.getDealPrice());
        vo.setDepositAmount(order.getDepositAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(STATUS_MAP.getOrDefault(order.getStatus(), "未知"));
        vo.setPayDeadline(order.getPayDeadline());
        vo.setPaidAt(order.getPaidAt());
        vo.setShippedAt(order.getShippedAt());
        vo.setCompletedAt(order.getCompletedAt());
        vo.setClosedAt(order.getClosedAt());
        vo.setCloseReason(order.getCloseReason());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }

    private String buildOrderNo(long orderId, LocalDateTime now) {
        String suffix = String.valueOf(orderId);
        suffix = suffix.substring(Math.max(0, suffix.length() - 8));
        return "OD" + ORDER_NO_TIME.format(now) + suffix;
    }

    private void sendOrderTimeoutMessage(BizOrder order) {
        long ttlMs = Duration.between(LocalDateTime.now(), order.getPayDeadline()).toMillis();
        if (ttlMs < 1000) {
            ttlMs = 1000;
        }
        OrderTimeoutMessage msg = OrderTimeoutMessage.builder()
                .orderId(order.getId())
                .expectedPayDeadlineMs(order.getPayDeadline()
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
                .build();
        String expiration = String.valueOf(ttlMs);
        MessagePostProcessor ttlSetter = message -> {
            message.getMessageProperties().setExpiration(expiration);
            return message;
        };
        rabbitTemplate.convertAndSend(
                MqConstants.EXCHANGE_DIRECT,
                MqConstants.RK_ORDER_TIMEOUT_DELAY,
                msg,
                ttlSetter);
    }

    private void sendCreditEvent(String eventType, Long userId, Long orderId) {
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_DIRECT, MqConstants.RK_CREDIT_EVENT,
                CreditEventMessage.builder()
                        .eventType(eventType)
                        .userId(userId)
                        .relatedId(String.valueOf(orderId))
                        .build());
    }
}
