package com.auction.business.controller;

import com.auction.business.service.FavoriteService;
import com.auction.business.vo.AuctionItemVO;
import com.auction.common.core.Result;
import com.auction.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/api/me/favorites")
    public Result<IPage<AuctionItemVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(favoriteService.listMyFavorites(SecurityUtils.getUserId(), page, size));
    }

    @GetMapping("/api/items/{id}/favorite")
    public Result<FavoriteStatusVO> status(@PathVariable Long id) {
        boolean favorited = favoriteService.isFavorite(SecurityUtils.getUserId(), id);
        return Result.success(new FavoriteStatusVO(favorited));
    }

    @PostMapping("/api/items/{id}/favorite")
    public Result<Void> add(@PathVariable Long id) {
        favoriteService.addFavorite(SecurityUtils.getUserId(), id);
        return Result.success(null);
    }

    @DeleteMapping("/api/items/{id}/favorite")
    public Result<Void> remove(@PathVariable Long id) {
        favoriteService.removeFavorite(SecurityUtils.getUserId(), id);
        return Result.success(null);
    }

    @Data
    public static class FavoriteStatusVO {
        private final boolean favorited;
    }
}
