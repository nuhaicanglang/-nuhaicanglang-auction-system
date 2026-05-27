package com.auction.business.service.impl;

import com.auction.business.dto.ReviewCreateDTO;
import com.auction.business.dto.ReviewQueryDTO;
import com.auction.business.entity.BizOrder;
import com.auction.business.entity.BizReview;
import com.auction.business.mapper.BizOrderMapper;
import com.auction.business.mapper.BizReviewMapper;
import com.auction.business.service.ReviewService;
import com.auction.business.vo.ReviewVO;
import com.auction.common.exception.BizException;
import com.auction.common.util.SnowflakeIdWorker;
import com.auction.mq.constant.MqConstants;
import com.auction.mq.message.CreditEventMessage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final BizReviewMapper reviewMapper;
    private final BizOrderMapper orderMapper;
    private final RabbitTemplate rabbitTemplate;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewVO createReview(Long orderId, Long userId, ReviewCreateDTO dto) {
        BizOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(50001, "订单不存在");
        }
        if (!Integer.valueOf(4).equals(order.getStatus())) {
            throw new BizException(50003, "订单完成后才能评价");
        }
        boolean buyer = order.getBuyerId().equals(userId);
        boolean seller = order.getSellerId().equals(userId);
        if (!buyer && !seller) {
            throw new BizException(50002, "无权评价该订单");
        }

        LocalDateTime now = LocalDateTime.now();
        BizReview review = new BizReview();
        review.setId(idWorker.nextId());
        review.setOrderId(orderId);
        review.setItemId(order.getItemId());
        review.setReviewerId(userId);
        review.setRevieweeId(buyer ? order.getSellerId() : order.getBuyerId());
        review.setRoleType(buyer ? "BUYER" : "SELLER");
        review.setScore(dto.getScore());
        review.setContent(dto.getContent());
        review.setStatus(1);
        review.setTenantId(0L);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException e) {
            throw new BizException(50008, "该订单已经评价过");
        }

        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_DIRECT, MqConstants.RK_CREDIT_EVENT,
                CreditEventMessage.builder()
                        .eventType("REVIEW_POSTED")
                        .userId(userId)
                        .relatedId(String.valueOf(review.getId()))
                        .build());
        return toVO(review);
    }

    @Override
    public IPage<ReviewVO> listReviews(ReviewQueryDTO query) {
        LambdaQueryWrapper<BizReview> wrapper = new LambdaQueryWrapper<>();
        if (query.getUserId() != null) {
            wrapper.eq(BizReview::getRevieweeId, query.getUserId());
        }
        if (query.getItemId() != null) {
            wrapper.eq(BizReview::getItemId, query.getItemId());
        }
        wrapper.eq(BizReview::getStatus, 1).orderByDesc(BizReview::getCreatedAt);
        Page<BizReview> page = new Page<>(query.getPage(), query.getSize());
        return reviewMapper.selectPage(page, wrapper).convert(this::toVO);
    }

    private ReviewVO toVO(BizReview review) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setItemId(review.getItemId());
        vo.setReviewerId(review.getReviewerId());
        vo.setRevieweeId(review.getRevieweeId());
        vo.setRoleType(review.getRoleType());
        vo.setScore(review.getScore());
        vo.setContent(review.getContent());
        vo.setStatus(review.getStatus());
        vo.setCreatedAt(review.getCreatedAt());
        return vo;
    }
}
