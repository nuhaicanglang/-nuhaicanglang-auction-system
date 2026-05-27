package com.auction.business.controller;

import com.auction.business.dto.CreditAdjustDTO;
import com.auction.business.dto.CreditLogQueryDTO;
import com.auction.business.service.CreditService;
import com.auction.business.vo.CreditLogVO;
import com.auction.business.vo.CreditVO;
import com.auction.common.core.Result;
import com.auction.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/credit")
public class AdminCreditController {

    private final CreditService creditService;

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<CreditVO> get(@PathVariable Long userId) {
        return Result.success(creditService.getCredit(userId));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<IPage<CreditLogVO>> logs(CreditLogQueryDTO query) {
        return Result.success(creditService.listAllLogs(query));
    }

    @PostMapping("/users/{userId}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Void> adjust(@PathVariable Long userId, @Valid @RequestBody CreditAdjustDTO dto) {
        creditService.adminAdjust(userId, SecurityUtils.getUserId(), dto);
        return Result.success();
    }
}
