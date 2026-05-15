package com.auction.business.vo;

import lombok.Data;

import java.util.List;

/**
 * 分类视图对象（含子分类，用于树形结构返回）。
 * children 字段递归嵌套，前端可直接渲染树形菜单。
 */
@Data
public class CategoryVO {

    private Long id;
    private Long parentId;
    private String path;
    private Integer level;
    private String name;
    private String icon;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private Integer itemCount;

    /** 子分类，叶子节点为空列表 */
    private List<CategoryVO> children;
}
