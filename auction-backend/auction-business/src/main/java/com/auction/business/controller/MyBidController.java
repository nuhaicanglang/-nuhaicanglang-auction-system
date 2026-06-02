package com.auction.business.controller;

import com.auction.business.service.BidService;
import com.auction.business.vo.MyBidVO;
import com.auction.common.core.Result;
import com.auction.framework.security.LoginUser;
import com.auction.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户竞拍流水接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/bids")
public class MyBidController {

    private final BidService bidService;

    @GetMapping
    public Result<IPage<MyBidVO>> listMyBids(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        LoginUser user = SecurityUtils.getLoginUser();
        return Result.success(bidService.listMyBids(user.getUserId(), page, size));
    }
}
