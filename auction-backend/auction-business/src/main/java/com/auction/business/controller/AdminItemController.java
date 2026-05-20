package com.auction.business.controller;

import com.auction.business.dto.AuctionItemQueryDTO;
import com.auction.business.entity.BizAuctionItem;
import com.auction.business.service.AuctionItemService;
import com.auction.business.vo.AuctionItemVO;
import com.auction.common.core.Result;
import com.auction.common.exception.BizException;
import com.auction.framework.annotation.Log;
import com.auction.framework.redis.RedisKey;
import com.auction.framework.security.SecurityUtils;
import com.auction.mq.constant.MqConstants;
import com.auction.mq.message.AuctionSettleMessage;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 商品管理端接口（管理员审核、强制下架等）。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/items")
public class AdminItemController {

    private final AuctionItemService auctionItemService;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;

    /**
     * 待审核商品列表。
     */
    @GetMapping("/audits")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<IPage<AuctionItemVO>> auditList(AuctionItemQueryDTO query) {
        query.setStatus(1); // 待审核
        return Result.success(auctionItemService.listItems(query));
    }

    /**
     * 审核商品（通过/驳回）。
     */
    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Log(module = "商品审核", businessType = "EDIT", description = "审核商品")
    public Result<Void> audit(@PathVariable Long id, @RequestBody AuditDTO dto) {
        BizAuctionItem item = auctionItemService.getById(id);
        if (item == null) {
            throw new BizException(30003, "商品不存在");
        }
        if (item.getStatus() != 1) {
            throw new BizException(30004, "该商品不在待审核状态");
        }

        Long adminId = SecurityUtils.getUserId();

        if ("PASS".equalsIgnoreCase(dto.getAction())) {
            item.setAuditStatus(1);
            // 计算原始拍卖时长，重新设置开始/结束时间
            long durationMin = Duration.between(item.getStartTime(), item.getEndTime()).toMinutes();
            LocalDateTime now = LocalDateTime.now();
            item.setStartTime(now);
            item.setEndTime(now.plusMinutes(durationMin));
            item.setStatus(3); // 直接进入拍卖中

            // 预热 Redis 当前价
            redisTemplate.opsForValue().set(
                    RedisKey.itemPrice(item.getId()),
                    item.getCurrentPrice().toPlainString());

            // 投递延迟消息：到期后触发结算
            long ttlMs = Duration.between(now, item.getEndTime()).toMillis();
            if (ttlMs < 1000) ttlMs = 1000; // 至少 1s
            AuctionSettleMessage settleMsg = AuctionSettleMessage.builder()
                    .itemId(item.getId())
                    .expectedEndTimeMs(item.getEndTime()
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    .build();
            String expiration = String.valueOf(ttlMs);
            MessagePostProcessor ttlSetter = message -> {
                message.getMessageProperties().setExpiration(expiration);
                return message;
            };
            rabbitTemplate.convertAndSend(
                    MqConstants.EXCHANGE_DIRECT,
                    MqConstants.RK_AUCTION_DELAY,
                    settleMsg,
                    ttlSetter);
        } else {
            item.setAuditStatus(2);
            // 状态保持待审（卖家可修改后重新提交）
        }
        item.setAuditRemark(dto.getRemark());
        item.setAuditBy(adminId);
        item.setAuditAt(LocalDateTime.now());
        item.setUpdatedBy(adminId);

        auctionItemService.updateById(item);
        return Result.success(null);
    }

    /**
     * 强制下架商品。
     */
    @PostMapping("/{id}/force-offline")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Log(module = "商品审核", businessType = "EDIT", description = "强制下架商品")
    public Result<Void> forceOffline(@PathVariable Long id) {
        BizAuctionItem item = auctionItemService.getById(id);
        if (item == null) {
            throw new BizException(30003, "商品不存在");
        }
        item.setStatus(7);
        item.setUpdatedBy(SecurityUtils.getUserId());
        auctionItemService.updateById(item);
        return Result.success(null);
    }

    @Data
    public static class AuditDTO {
        /** PASS / REJECT */
        private String action;
        private String remark;
    }
}
