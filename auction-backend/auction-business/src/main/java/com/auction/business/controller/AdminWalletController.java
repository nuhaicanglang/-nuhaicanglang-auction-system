package com.auction.business.controller;

import com.auction.business.dto.WalletAdjustDTO;
import com.auction.business.dto.WalletTransactionQueryDTO;
import com.auction.business.service.WalletService;
import com.auction.business.vo.WalletSummaryVO;
import com.auction.business.vo.WalletTransactionVO;
import com.auction.common.core.Result;
import com.auction.framework.annotation.Log;
import com.auction.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端钱包接口。
 * 管理员可调账、查看全平台流水和资金汇总。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminWalletController {

    private final WalletService walletService;

    @PostMapping("/users/{userId}/wallet/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Log(module = "钱包管理", businessType = "EDIT", description = "管理员钱包调账")
    public Result<Void> adjust(@PathVariable Long userId,
                               @Valid @RequestBody WalletAdjustDTO dto,
                               HttpServletRequest request) {
        String requestKey = request.getHeader("X-Idempotent-Key");
        if (!StringUtils.hasText(dto.getIdempotentKey()) && StringUtils.hasText(requestKey)) {
            dto.setIdempotentKey(requestKey);
        }
        walletService.adminAdjust(userId, SecurityUtils.getUserId(), dto);
        return Result.success();
    }

    @GetMapping("/wallet/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<IPage<WalletTransactionVO>> transactions(WalletTransactionQueryDTO query) {
        return Result.success(walletService.listAllTransactions(query));
    }

    @GetMapping("/wallet/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<WalletSummaryVO> summary() {
        return Result.success(walletService.getSummary());
    }
}
