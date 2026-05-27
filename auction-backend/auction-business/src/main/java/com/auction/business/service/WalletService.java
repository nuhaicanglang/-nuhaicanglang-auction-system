package com.auction.business.service;

import com.auction.business.dto.WalletAdjustCmd;
import com.auction.business.dto.WalletAdjustDTO;
import com.auction.business.dto.WalletTransactionQueryDTO;
import com.auction.business.entity.BizWalletTransaction;
import com.auction.business.vo.WalletSummaryVO;
import com.auction.business.vo.WalletTransactionVO;
import com.auction.business.vo.WalletVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;

/**
 * 钱包服务接口。
 */
public interface WalletService {

    BizWalletTransaction adjust(WalletAdjustCmd cmd);

    WalletVO getWallet(Long userId);

    IPage<WalletTransactionVO> listMyTransactions(Long userId, WalletTransactionQueryDTO query);

    IPage<WalletTransactionVO> listAllTransactions(WalletTransactionQueryDTO query);

    WalletSummaryVO getSummary();

    void adminAdjust(Long targetUserId, Long adminId, WalletAdjustDTO dto);

    boolean freezeBidDeposit(Long userId, Long itemId, BigDecimal amount, String requestId);

    void cancelBidDepositFreeze(Long userId, Long itemId, BigDecimal amount, String requestId);

    void settleBidDeposits(Long itemId, Long winnerId, BigDecimal depositAmount);
}
