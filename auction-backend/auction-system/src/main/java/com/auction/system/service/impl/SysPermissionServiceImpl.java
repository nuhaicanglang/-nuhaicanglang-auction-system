package com.auction.system.service.impl;

import com.auction.system.entity.SysPermission;
import com.auction.system.entity.SysRolePermission;
import com.auction.system.mapper.SysPermissionMapper;
import com.auction.system.mapper.SysRolePermissionMapper;
import com.auction.system.service.SysPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限业务实现类。
 */
@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements SysPermissionService {

    private final SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    public List<SysPermission> listAll() {
        return list(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getSortOrder));
    }

    @Override
    public List<String> listPermissionCodesByUserId(Long userId) {
        return baseMapper.selectPermissionsByUserId(userId)
                .stream()
                .map(SysPermission::getCode)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> listPermissionIdsByRoleId(Long roleId) {
        return sysRolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId))
                .stream()
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toList());
    }

    /**
     * 给角色分配权限：先清除原有关联，再批量插入。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permId : permissionIds) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                rp.setCreatedAt(LocalDateTime.now());
                sysRolePermissionMapper.insert(rp);
            }
        }
    }
}
