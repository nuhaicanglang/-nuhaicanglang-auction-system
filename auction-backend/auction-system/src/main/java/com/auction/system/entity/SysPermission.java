package com.auction.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体类，对应数据库表 sys_permission。
 * 权限可以是菜单（type=1）、按钮（type=2）或 API 接口（type=3）。
 * parent_id 形成树形结构，便于前端渲染菜单和后端做权限校验。
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 父权限ID，0表示顶级节点。
     */
    private Long parentId;

    /**
     * 权限编码，如 system:user:list，用于 @PreAuthorize 校验。
     */
    private String code;

    /**
     * 权限显示名称。
     */
    private String name;

    /**
     * 权限类型：1菜单 / 2按钮 / 3API。
     */
    private Integer type;

    /**
     * 前端路由路径，仅菜单类型使用。
     */
    private String path;

    /**
     * 前端组件路径，仅菜单类型使用。
     */
    private String component;

    /**
     * 菜单图标。
     */
    private String icon;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
