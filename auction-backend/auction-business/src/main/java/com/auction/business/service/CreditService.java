package com.auction.business.service;

import com.auction.business.dto.CreditAdjustDTO;
import com.auction.business.dto.CreditApplyCmd;
import com.auction.business.dto.CreditLogQueryDTO;
import com.auction.business.vo.CreditLogVO;
import com.auction.business.vo.CreditVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface CreditService {

    CreditVO getCredit(Long userId);

    IPage<CreditLogVO> listMyLogs(Long userId, CreditLogQueryDTO query);

    IPage<CreditLogVO> listAllLogs(CreditLogQueryDTO query);

    void applyEvent(String eventType, Long userId, String relatedId);

    void apply(CreditApplyCmd cmd);

    void adminAdjust(Long userId, Long adminId, CreditAdjustDTO dto);

    void restoreMonthlyCredit();
}
