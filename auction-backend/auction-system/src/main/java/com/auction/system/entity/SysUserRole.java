package com.auction.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户—角色关联实体，对应数据库表 sys_user_role。
 * 多对多关系的中间表：一个用户可以有多个角色，一个角色也可以分配给多个用户。
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    /**
     * 用户ID，与 role_id 共同组成联合主键。
     */
    private Long userId;

    /**
     * 角色ID。
     */
    private Long roleId;

    private LocalDateTime createdAt;
}
