package com.auction.search.controller;

import com.auction.common.core.Result;
import com.auction.framework.security.SecurityUtils;
import com.auction.search.dto.ItemSearchQueryDTO;
import com.auction.search.service.ItemSearchService;
import com.auction.search.service.SearchHistoryService;
import com.auction.search.vo.ItemHitVO;
import com.auction.search.vo.ItemSearchResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品搜索接口。
 * <ul>
 *   <li>GET /api/search/items         全文检索 + 过滤 + 高亮 + 分面聚合（匿名可用）</li>
 *   <li>GET /api/search/suggest       关键词联想（匿名可用）</li>
 *   <li>GET /api/search/history       当前用户最近搜索（需登录）</li>
 *   <li>DELETE /api/search/history    清空当前用户搜索（需登录）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ItemSearchService itemSearchService;
    private final SearchHistoryService searchHistoryService;

    /** 搜索商品 */
    @GetMapping("/items")
    public Result<ItemSearchResultVO> searchItems(@ModelAttribute ItemSearchQueryDTO query) {
        Long userId = SecurityUtils.getUserId();
        return Result.success(itemSearchService.search(query, userId));
    }

    /** 关键词联想（前缀匹配） */
    @GetMapping("/suggest")
    public Result<List<ItemHitVO>> suggest(@RequestParam("prefix") String prefix,
                                            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return Result.success(itemSearchService.suggest(prefix, size == null ? 10 : size));
    }

    /** 获取当前用户搜索历史 */
    @GetMapping("/history")
    public Result<List<String>> getHistory() {
        Long userId = SecurityUtils.getUserId();
        return Result.success(searchHistoryService.getHistory(userId));
    }

    /** 清空当前用户搜索历史 */
    @DeleteMapping("/history")
    public Result<Void> clearHistory() {
        Long userId = SecurityUtils.getUserId();
        searchHistoryService.clearHistory(userId);
        return Result.success();
    }
}
