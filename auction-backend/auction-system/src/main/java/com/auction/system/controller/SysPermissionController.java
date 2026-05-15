package com.auction.system.controller;

import com.auction.common.core.Result;
import com.auction.system.convert.SysPermissionConvert;
import com.auction.system.service.SysPermissionService;
import com.auction.system.vo.SysPermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理接口（管理端）。
 * 主要用于展示权限树，供角色分配权限时使用。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/permissions")
public class SysPermissionController {

    private final SysPermissionService sysPermissionService;

    /**
     * 查询权限树。
     * 返回树形结构的权限列表，前端可直接渲染为菜单树或权限选择器。
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<SysPermissionVO>> permissionTree() {
        return Result.success(SysPermissionConvert.buildTree(sysPermissionService.listAll()));
    }
}
