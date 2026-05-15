package com.auction.system.service;

import com.auction.system.entity.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 角色业务接口。
 * 提供角色 CRUD 和用户角色分配能力。
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 查询所有可用角色列表。
     */
    List<SysRole> listAllRoles();

    /**
     * 查询指定用户拥有的角色列表。
     */
    List<SysRole> listRolesByUserId(Long userId);

    /**
     * 给用户分配角色（全量覆盖：先删除原有角色，再插入新角色）。
     *
     * @param userId  目标用户ID
     * @param roleIds 要分配的角色ID列表
     */
    void assignRolesToUser(Long userId, List<Long> roleIds);
}
