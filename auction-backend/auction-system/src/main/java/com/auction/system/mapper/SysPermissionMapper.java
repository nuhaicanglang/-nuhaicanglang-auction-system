package com.auction.system.mapper;

import com.auction.system.entity.SysPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限 Mapper。
 * 提供按角色ID列表查询权限的方法，用于登录时加载用户的完整权限码集合。
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询指定用户拥有的所有权限（通过角色关联）。
     * 用户 → 角色 → 权限，三表联查，去重返回。
     */
    @Select("<script>" +
            "SELECT DISTINCT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON rp.permission_id = p.id " +
            "INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id " +
            "WHERE ur.user_id = #{userId} AND p.status = 1 AND p.deleted = 0" +
            "</script>")
    List<SysPermission> selectPermissionsByUserId(@Param("userId") Long userId);
}
