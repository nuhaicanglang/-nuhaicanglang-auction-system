package com.auction.business.controller;

import com.auction.business.dto.AuctionItemQueryDTO;
import com.auction.business.entity.BizAuctionItem;
import com.auction.business.service.AuctionItemService;
import com.auction.business.vo.AuctionItemVO;
import com.auction.common.core.Result;
import com.auction.common.exception.BizException;
import com.auction.framework.annotation.Log;
import com.auction.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 商品管理端接口（管理员审核、强制下架等）。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/items")
public class AdminItemController {

    private final AuctionItemService auctionItemService;

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
            item.setStatus(2); // 待开拍
            // 审核通过后计算开拍时间
            item.setStartTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now().plusMinutes(
                    java.time.Duration.between(item.getStartTime(), item.getEndTime()).toMinutes()));
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
