package com.auction.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建/编辑分类请求参数。
 */
@Data
public class CategoryCreateDTO {

    /** 父分类 ID，根节点传 0 */
    @NotNull(message = "父分类ID不能为空")
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String icon;

    private String description;

    private Integer sortOrder;
}
