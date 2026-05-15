package com.auction.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 权限返回对象，支持树形结构展示。
 */
@Data
public class SysPermissionVO {

    private Long id;

    private Long parentId;

    private String code;

    private String name;

    private Integer type;

    private String path;

    private String icon;

    private Integer sortOrder;

    private Integer status;

    /**
     * 子权限列表，树形结构时使用。
     */
    private List<SysPermissionVO> children;
}
