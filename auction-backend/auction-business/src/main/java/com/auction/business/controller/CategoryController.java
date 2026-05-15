package com.auction.business.controller;

import com.auction.business.service.CategoryService;
import com.auction.business.vo.CategoryVO;
import com.auction.common.core.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类公开接口（用户端，无需登录）。
 * 返回完整的分类树，前端用于渲染导航菜单。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取分类树（Redis 缓存，高频接口）。
     */
    @GetMapping("/tree")
    public Result<List<CategoryVO>> getCategoryTree() {
        return Result.success(categoryService.getCategoryTree());
    }
}
