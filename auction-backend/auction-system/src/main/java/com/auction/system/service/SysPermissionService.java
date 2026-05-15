package com.auction.system.service;

import com.auction.system.entity.SysPermission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 权限业务接口。
 * 提供权限查询能力，包括按用户查询权限码和获取权限树。
 */
public interface SysPermissionService extends IService<SysPermission> {

    /**
     * 查询所有权限（树形展示用）。
     */
    List<SysPermission> listAll();

    /**
     * 查询指定用户拥有的所有权限编码。
     */
    List<String> listPermissionCodesByUserId(Long userId);

    /**
     * 查询指定角色拥有的权限ID列表。
     */
    List<Long> listPermissionIdsByRoleId(Long roleId);

    /**
     * 给角色分配权限（全量覆盖）。
     */
    void assignPermissionsToRole(Long roleId, List<Long> permissionIds);
}
