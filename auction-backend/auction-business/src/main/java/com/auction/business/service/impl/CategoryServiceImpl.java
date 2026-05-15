package com.auction.business.service.impl;

import com.auction.business.dto.CategoryCreateDTO;
import com.auction.business.entity.BizCategory;
import com.auction.business.mapper.BizCategoryMapper;
import com.auction.business.service.CategoryService;
import com.auction.business.vo.CategoryVO;
import com.auction.common.core.ErrorCode;
import com.auction.common.exception.BizException;
import com.auction.common.util.SnowflakeIdWorker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 分类服务实现。
 * <p>
 * 核心设计思路：
 * 1. 数据库存全量分类（包含 parent_id 和 path）；
 * 2. 服务启动或数据变更后，将分类树 JSON 序列化写入 Redis，TTL 24 小时；
 * 3. 每次查询先走 Redis，缓存失效再查库重建缓存（Cache-Aside 模式）；
 * 4. 树构造采用非递归：先按 parent_id 分组，再从根节点遍历。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<BizCategoryMapper, BizCategory>
        implements CategoryService {

    private static final String CACHE_KEY = "cache:category:tree";
    private static final long CACHE_TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    @Override
    public List<CategoryVO> getCategoryTree() {
        // 1. 先查 Redis 缓存
        String cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<CategoryVO>>() {});
            } catch (Exception e) {
                log.warn("分类树缓存反序列化失败，重建", e);
            }
        }

        // 2. 缓存未命中，从数据库加载
        List<BizCategory> all = list(new LambdaQueryWrapper<BizCategory>()
                .eq(BizCategory::getDeleted, 0)
                .eq(BizCategory::getStatus, 1)
                .orderByAsc(BizCategory::getSortOrder));

        List<CategoryVO> tree = buildTree(all, 0L);

        // 3. 结果写回 Redis
        try {
            String json = objectMapper.writeValueAsString(tree);
            redisTemplate.opsForValue().set(CACHE_KEY, json, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("分类树写入缓存失败", e);
        }

        return tree;
    }

    @Override
    public void createCategory(CategoryCreateDTO dto) {
        BizCategory parent = null;
        if (dto.getParentId() != 0) {
            parent = getById(dto.getParentId());
            if (parent == null) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "父分类不存在");
            }
            if (parent.getLevel() >= 3) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "最多支持三级分类");
            }
        }

        BizCategory category = new BizCategory();
        category.setId(idWorker.nextId());
        category.setParentId(dto.getParentId());
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setDescription(dto.getDescription());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        category.setStatus(1);
        category.setItemCount(0);

        if (parent == null) {
            category.setLevel(1);
            category.setPath(String.valueOf(category.getId()));
        } else {
            category.setLevel(parent.getLevel() + 1);
            category.setPath(parent.getPath() + "/" + category.getId());
        }

        save(category);
        evictCache();
    }

    @Override
    public void updateCategory(Long id, CategoryCreateDTO dto) {
        BizCategory category = getById(id);
        if (category == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "分类不存在");
        }
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) {
            category.setSortOrder(dto.getSortOrder());
        }
        updateById(category);
        evictCache();
    }

    @Override
    public void deleteCategory(Long id) {
        // 有子节点则拒绝删除
        long childCount = count(new LambdaQueryWrapper<BizCategory>()
                .eq(BizCategory::getParentId, id)
                .eq(BizCategory::getDeleted, 0));
        if (childCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "请先删除子分类");
        }
        removeById(id);
        evictCache();
    }

    @Override
    public void toggleStatus(Long id) {
        BizCategory category = getById(id);
        if (category == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "分类不存在");
        }
        category.setStatus(category.getStatus() == 1 ? 0 : 1);
        updateById(category);
        evictCache();
    }

    @Override
    public void evictCache() {
        redisTemplate.delete(CACHE_KEY);
    }

    /**
     * 非递归构造分类树。
     * 先将所有分类按 parent_id 分组，再从根节点开始广度优先组装 children。
     */
    private List<CategoryVO> buildTree(List<BizCategory> all, Long rootParentId) {
        Map<Long, List<CategoryVO>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        BizCategory::getParentId,
                        Collectors.mapping(this::toVO, Collectors.toList())));

        List<CategoryVO> roots = grouped.getOrDefault(rootParentId, new ArrayList<>());
        fillChildren(roots, grouped);
        return roots;
    }

    private void fillChildren(List<CategoryVO> nodes, Map<Long, List<CategoryVO>> grouped) {
        for (CategoryVO node : nodes) {
            List<CategoryVO> children = grouped.getOrDefault(node.getId(), new ArrayList<>());
            node.setChildren(children);
            if (!children.isEmpty()) {
                fillChildren(children, grouped);
            }
        }
    }

    private CategoryVO toVO(BizCategory entity) {
        CategoryVO vo = new CategoryVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setPath(entity.getPath());
        vo.setLevel(entity.getLevel());
        vo.setName(entity.getName());
        vo.setIcon(entity.getIcon());
        vo.setDescription(entity.getDescription());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setItemCount(entity.getItemCount());
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}
