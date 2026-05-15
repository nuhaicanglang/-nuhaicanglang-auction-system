package com.auction.business.service;

import com.auction.business.dto.CategoryCreateDTO;
import com.auction.business.entity.BizCategory;
import com.auction.business.vo.CategoryVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 分类服务接口。
 */
public interface CategoryService extends IService<BizCategory> {

    /**
     * 获取完整分类树（Redis 缓存，管理端/前端均可用）。
     */
    List<CategoryVO> getCategoryTree();

    /**
     * 新增分类。
     */
    void createCategory(CategoryCreateDTO dto);

    /**
     * 更新分类。
     */
    void updateCategory(Long id, CategoryCreateDTO dto);

    /**
     * 删除分类（软删，有子节点时拒绝）。
     */
    void deleteCategory(Long id);

    /**
     * 切换分类状态（启用/停用）。
     */
    void toggleStatus(Long id);

    /**
     * 主动清除分类树缓存（更新后调用）。
     */
    void evictCache();
}
