package com.auction.business.controller;

import com.auction.business.dto.CategoryCreateDTO;
import com.auction.business.service.CategoryService;
import com.auction.business.vo.CategoryVO;
import com.auction.common.core.Result;
import com.auction.framework.annotation.Log;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理端接口（需要 ADMIN 及以上权限）。
 * 支持完整的 CRUD 和状态切换操作。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    /**
     * 分类树（管理端，包含停用节点）。
     * 直接查库（不过滤 status），方便管理员查看全量分类。
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<CategoryVO>> getCategoryTree() {
        return Result.success(categoryService.getCategoryTree());
    }

    /**
     * 新增分类。
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Log(module = "分类管理", businessType = "NEW", description = "新增分类")
    public Result<Void> create(@Valid @RequestBody CategoryCreateDTO dto) {
        categoryService.createCategory(dto);
        return Result.success(null);
    }

    /**
     * 编辑分类（名称/图标/描述/排序）。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Log(module = "分类管理", businessType = "EDIT", description = "编辑分类")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody CategoryCreateDTO dto) {
        categoryService.updateCategory(id, dto);
        return Result.success(null);
    }

    /**
     * 删除分类（有子分类时拒绝）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Log(module = "分类管理", businessType = "DELETE", description = "删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success(null);
    }

    /**
     * 切换分类状态（启用/停用）。
     */
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Log(module = "分类管理", businessType = "EDIT", description = "切换分类状态")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        categoryService.toggleStatus(id);
        return Result.success(null);
    }
}
