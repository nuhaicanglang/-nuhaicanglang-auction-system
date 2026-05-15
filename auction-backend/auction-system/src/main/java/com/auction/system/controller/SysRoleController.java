package com.auction.system.controller;

import com.auction.common.core.Result;
import com.auction.system.convert.SysRoleConvert;
import com.auction.system.service.SysPermissionService;
import com.auction.system.service.SysRoleService;
import com.auction.system.vo.SysRoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理接口（管理端）。
 * 所有接口都需要 ADMIN 或 SUPER_ADMIN 角色才能访问。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/roles")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    private final SysPermissionService sysPermissionService;

    /**
     * 查询所有角色列表。
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<SysRoleVO>> listRoles() {
        List<SysRoleVO> voList = sysRoleService.listAllRoles().stream()
                .map(SysRoleConvert::toVO)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 查询指定角色拥有的权限ID列表。
     * 前端编辑角色权限时，先调用此接口回显已勾选的权限。
     */
    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<Long>> getRolePermissions(@PathVariable Long roleId) {
        return Result.success(sysPermissionService.listPermissionIdsByRoleId(roleId));
    }

    /**
     * 给角色分配权限。
     * 传入权限ID列表，全量覆盖该角色的权限。
     */
    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Void> assignPermissions(@PathVariable Long roleId,
                                          @RequestBody List<Long> permissionIds) {
        sysPermissionService.assignPermissionsToRole(roleId, permissionIds);
        return Result.success(null);
    }
}
