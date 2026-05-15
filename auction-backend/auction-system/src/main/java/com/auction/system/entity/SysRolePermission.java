package com.auction.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色—权限关联实体，对应数据库表 sys_role_permission。
 * 多对多关系的中间表：一个角色可以有多个权限，一个权限也可以分配给多个角色。
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission {

    /**
     * 角色ID，与 permission_id 共同组成联合主键。
     */
    private Long roleId;

    /**
     * 权限ID。
     */
    private Long permissionId;

    private LocalDateTime createdAt;
}
