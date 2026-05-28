package com.auction.business.controller;

import com.auction.business.service.StatsService;
import com.auction.business.vo.CategoryHotVO;
import com.auction.business.vo.ItemTopVO;
import com.auction.business.vo.StatsOverviewVO;
import com.auction.business.vo.StatsTrendPointVO;
import com.auction.common.core.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理仪表盘接口。
 * <ul>
 *   <li>GET /api/admin/stats/overview          今日/累计指标</li>
 *   <li>GET /api/admin/stats/trend?days=30     趋势图</li>
 *   <li>GET /api/admin/stats/categories?limit  热门分类</li>
 *   <li>GET /api/admin/stats/items/top?limit   TOP 商品</li>
 * </ul>
 * 仅 ADMIN/SUPER_ADMIN 可访问。
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminStatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<StatsOverviewVO> overview() {
        return Result.success(statsService.overview());
    }

    @GetMapping("/trend")
    public Result<List<StatsTrendPointVO>> trend(
            @RequestParam(value = "days", defaultValue = "30") Integer days) {
        return Result.success(statsService.trend(days == null ? 30 : days));
    }

    @GetMapping("/categories")
    public Result<List<CategoryHotVO>> hotCategories(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return Result.success(statsService.hotCategories(limit == null ? 10 : limit));
    }

    @GetMapping("/items/top")
    public Result<List<ItemTopVO>> topItems(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return Result.success(statsService.topItems(limit == null ? 10 : limit));
    }
}
