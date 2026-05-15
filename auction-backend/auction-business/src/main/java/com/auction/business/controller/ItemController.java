package com.auction.business.controller;

import com.auction.business.dto.AuctionItemCreateDTO;
import com.auction.business.dto.AuctionItemQueryDTO;
import com.auction.business.service.AuctionItemService;
import com.auction.business.vo.AuctionItemVO;
import com.auction.common.core.Result;
import com.auction.framework.annotation.Log;
import com.auction.framework.security.LoginUser;
import com.auction.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 拍卖商品用户端接口。
 * 列表和详情对外公开，发布/编辑/下架需要登录。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items")
public class ItemController {

    private final AuctionItemService auctionItemService;

    /**
     * 商品列表（分页 + 筛选 + 排序），公开接口。
     */
    @GetMapping
    public Result<IPage<AuctionItemVO>> list(AuctionItemQueryDTO query) {
        return Result.success(auctionItemService.listItems(query));
    }

    /**
     * 商品详情，公开接口。
     */
    @GetMapping("/{id}")
    public Result<AuctionItemVO> detail(@PathVariable Long id) {
        return Result.success(auctionItemService.getItemDetail(id));
    }

    /**
     * 发布商品（需登录）。
     */
    @PostMapping
    @Log(module = "商品管理", businessType = "NEW", description = "发布商品")
    public Result<Long> publish(@Valid @RequestBody AuctionItemCreateDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        Long itemId = auctionItemService.publishItem(dto, user.getUserId());
        return Result.success(itemId);
    }

    /**
     * 编辑商品（仅待审/驳回可改）。
     */
    @PutMapping("/{id}")
    @Log(module = "商品管理", businessType = "EDIT", description = "编辑商品")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody AuctionItemCreateDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        auctionItemService.updateItem(id, dto, user.getUserId());
        return Result.success(null);
    }

    /**
     * 下架自己的商品。
     */
    @PostMapping("/{id}/offline")
    @Log(module = "商品管理", businessType = "EDIT", description = "下架商品")
    public Result<Void> offline(@PathVariable Long id) {
        LoginUser user = SecurityUtils.getLoginUser();
        auctionItemService.offlineItem(id, user.getUserId());
        return Result.success(null);
    }

    /**
     * 我发布的商品（卖家视角）。
     */
    @GetMapping("/my")
    public Result<IPage<AuctionItemVO>> myItems(AuctionItemQueryDTO query) {
        LoginUser user = SecurityUtils.getLoginUser();
        query.setSellerId(user.getUserId());
        return Result.success(auctionItemService.listItems(query));
    }
}
