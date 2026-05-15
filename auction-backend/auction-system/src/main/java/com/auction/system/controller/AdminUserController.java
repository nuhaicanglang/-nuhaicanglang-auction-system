package com.auction.system.controller;

import com.auction.common.core.ErrorCode;
import com.auction.common.core.Result;
import com.auction.common.exception.BizException;
import com.auction.system.convert.SysRoleConvert;
import com.auction.system.convert.SysUserConvert;
import com.auction.system.dto.AssignRolesDTO;
import com.auction.system.entity.SysUser;
import com.auction.system.service.SysRoleService;
import com.auction.system.service.SysUserService;
import com.auction.system.vo.SysRoleVO;
import com.auction.system.vo.SysUserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理接口（管理端）。
 * 管理员可以查看用户列表、修改用户状态、分配角色等。
 * 所有接口都通过 @PreAuthorize 限制为 ADMIN 或 SUPER_ADMIN 角色。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final SysUserService sysUserService;

    private final SysRoleService sysRoleService;

    /**
     * 查询用户列表（简易版，后续可加分页和筛选）。
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<SysUserVO>> listUsers() {
        List<SysUserVO> voList = sysUserService.list().stream()
                .map(SysUserConvert::toVO)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 查询指定用户详情。
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<SysUserVO> getUserDetail(@PathVariable Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return Result.success(SysUserConvert.toVO(user));
    }

    /**
     * 查询指定用户的角色列表。
     */
    @GetMapping("/{userId}/roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<SysRoleVO>> getUserRoles(@PathVariable Long userId) {
        List<SysRoleVO> voList = sysRoleService.listRolesByUserId(userId).stream()
                .map(SysRoleConvert::toVO)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 给用户分配角色（全量覆盖）。
     * 仅超级管理员可以修改角色分配。
     */
    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Void> assignRoles(@PathVariable Long userId,
                                    @Valid @RequestBody AssignRolesDTO dto) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        sysRoleService.assignRolesToUser(userId, dto.getRoleIds());
        return Result.success(null);
    }

    /**
     * 修改用户状态（启用/禁用/拉黑）。
     *
     * @param status 目标状态：1正常 / 0禁用 / 2黑名单
     */
    @PutMapping("/{userId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Void> changeUserStatus(@PathVariable Long userId,
                                         @RequestParam Integer status) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(status);
        sysUserService.updateById(user);
        return Result.success(null);
    }
}
