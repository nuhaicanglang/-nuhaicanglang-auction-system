package com.auction.business.controller;

import com.auction.business.dto.BidDTO;
import com.auction.business.service.BidService;
import com.auction.business.vo.BidResultVO;
import com.auction.business.vo.BidVO;
import com.auction.common.core.Result;
import com.auction.framework.security.LoginUser;
import com.auction.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 出价接口。
 * POST /api/items/{itemId}/bids  出价（需登录）
 * GET  /api/items/{itemId}/bids  出价记录（公开）
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items/{itemId}/bids")
public class BidController {

    private final BidService bidService;

    /**
     * 出价。
     * 幂等ID 从 Header X-Idempotent-Key 获取，未传则服务端生成。
     */
    @PostMapping
    public Result<BidResultVO> placeBid(@PathVariable Long itemId,
                                        @Valid @RequestBody BidDTO dto,
                                        HttpServletRequest request) {
        LoginUser user = SecurityUtils.getLoginUser();

        String requestId = request.getHeader("X-Idempotent-Key");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String clientIp = request.getRemoteAddr();
        BidResultVO result = bidService.placeBid(itemId, user.getUserId(), dto.getPrice(), requestId, clientIp);
        return Result.success(result);
    }

    /**
     * 查询商品的出价记录（公开，按时间倒序）。
     */
    @GetMapping
    public Result<IPage<BidVO>> listBids(@PathVariable Long itemId,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return Result.success(bidService.listBids(itemId, page, size));
    }
}
