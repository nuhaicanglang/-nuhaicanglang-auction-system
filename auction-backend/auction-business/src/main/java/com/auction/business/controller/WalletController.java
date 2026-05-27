package com.auction.business.controller;

import com.auction.business.dto.WalletTransactionQueryDTO;
import com.auction.business.service.WalletService;
import com.auction.business.vo.WalletTransactionVO;
import com.auction.business.vo.WalletVO;
import com.auction.common.core.Result;
import com.auction.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人钱包接口。
 * 普通用户只能查看自己的钱包和资金流水。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/wallet")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public Result<WalletVO> getMyWallet() {
        return Result.success(walletService.getWallet(SecurityUtils.getUserId()));
    }

    @GetMapping("/transactions")
    public Result<IPage<WalletTransactionVO>> myTransactions(WalletTransactionQueryDTO query) {
        return Result.success(walletService.listMyTransactions(SecurityUtils.getUserId(), query));
    }
}
