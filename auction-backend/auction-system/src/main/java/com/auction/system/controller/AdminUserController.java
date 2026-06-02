package com.auction.system.controller;

import com.auction.common.core.ErrorCode;
import com.auction.common.core.Result;
import com.auction.common.exception.BizException;
import com.auction.framework.annotation.Log;
import com.auction.framework.security.SecurityUtils;
import com.auction.system.convert.SysRoleConvert;
import com.auction.system.convert.SysUserConvert;
import com.auction.system.dto.AssignRolesDTO;
import com.auction.system.entity.SysRole;
import com.auction.system.entity.SysUser;
import com.auction.system.service.SysRoleService;
import com.auction.system.service.SysUserService;
import com.auction.system.vo.SysRoleVO;
import com.auction.system.vo.SysUserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
                .map(user -> {
                    SysUserVO vo = SysUserConvert.toVO(user);
                    vo.setRoles(sysRoleService.listRolesByUserId(user.getId()).stream()
                            .map(SysRole::getCode)
                            .collect(Collectors.toList()));
                    return vo;
                })
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
    @Log(module = "用户管理", businessType = "EDIT", description = "修改用户状态")
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

    /**
     * 拉黑用户。
     * 设置 status=2，记录拉黑原因和操作人。
     * 拉黑后用户的 JWT 将无法通过登录校验（登录时检查 status）。
     */
    @PostMapping("/{userId}/blacklist")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Log(module = "用户管理", businessType = "EDIT", description = "拉黑用户")
    public Result<Void> blacklistUser(@PathVariable Long userId,
                                      @RequestBody Map<String, String> body) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(2);
        user.setBlacklistReason(body.getOrDefault("reason", ""));
        user.setBlacklistedBy(SecurityUtils.getUserId());
        user.setBlacklistedAt(LocalDateTime.now());
        sysUserService.updateById(user);
        return Result.success(null);
    }

    /**
     * 解除拉黑。
     * 恢复用户状态为正常，清除拉黑信息。
     */
    @DeleteMapping("/{userId}/blacklist")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Log(module = "用户管理", businessType = "EDIT", description = "解除拉黑")
    public Result<Void> unblacklistUser(@PathVariable Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(1);
        user.setBlacklistReason(null);
        user.setBlacklistedBy(null);
        user.setBlacklistedAt(null);
        sysUserService.updateById(user);
        return Result.success(null);
    }
}
