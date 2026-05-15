package com.auction.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体类，对应数据库表 sys_role。
 * RBAC 模型中角色是连接用户和权限的桥梁：用户拥有角色，角色拥有权限。
 */
@Data
@TableName("sys_role")
public class SysRole {

    /**
     * 主键ID。
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 角色编码，如 SUPER_ADMIN / ADMIN / USER，系统内唯一。
     */
    private String code;

    /**
     * 角色显示名称。
     */
    private String name;

    /**
     * 角色描述说明。
     */
    private String description;

    /**
     * 排序号，数字越小越靠前。
     */
    private Integer sortOrder;

    /**
     * 状态：1启用，0停用。
     */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
