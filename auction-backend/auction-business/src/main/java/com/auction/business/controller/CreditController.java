package com.auction.business.controller;

import com.auction.business.dto.CreditLogQueryDTO;
import com.auction.business.service.CreditService;
import com.auction.business.vo.CreditLogVO;
import com.auction.business.vo.CreditVO;
import com.auction.common.core.Result;
import com.auction.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/credit")
public class CreditController {

    private final CreditService creditService;

    @GetMapping
    public Result<CreditVO> myCredit() {
        return Result.success(creditService.getCredit(SecurityUtils.getUserId()));
    }

    @GetMapping("/logs")
    public Result<IPage<CreditLogVO>> myLogs(CreditLogQueryDTO query) {
        return Result.success(creditService.listMyLogs(SecurityUtils.getUserId(), query));
    }
}
