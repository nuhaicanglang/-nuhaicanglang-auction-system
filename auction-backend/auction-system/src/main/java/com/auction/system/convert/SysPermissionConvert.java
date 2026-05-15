package com.auction.system.convert;

import com.auction.system.entity.SysPermission;
import com.auction.system.vo.SysPermissionVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限对象转换工具，支持将扁平权限列表转换为树形结构。
 */
public final class SysPermissionConvert {

    private SysPermissionConvert() {
    }

    public static SysPermissionVO toVO(SysPermission perm) {
        if (perm == null) {
            return null;
        }
        SysPermissionVO vo = new SysPermissionVO();
        vo.setId(perm.getId());
        vo.setParentId(perm.getParentId());
        vo.setCode(perm.getCode());
        vo.setName(perm.getName());
        vo.setType(perm.getType());
        vo.setPath(perm.getPath());
        vo.setIcon(perm.getIcon());
        vo.setSortOrder(perm.getSortOrder());
        vo.setStatus(perm.getStatus());
        return vo;
    }

    /**
     * 将扁平的权限列表构建为树形结构。
     * 顶级节点的 parentId = 0。
     */
    public static List<SysPermissionVO> buildTree(List<SysPermission> permissions) {
        List<SysPermissionVO> voList = permissions.stream()
                .map(SysPermissionConvert::toVO)
                .collect(Collectors.toList());

        // 按 parentId 分组
        Map<Long, List<SysPermissionVO>> parentMap = voList.stream()
                .collect(Collectors.groupingBy(SysPermissionVO::getParentId));

        // 为每个节点挂载子节点
        for (SysPermissionVO vo : voList) {
            vo.setChildren(parentMap.getOrDefault(vo.getId(), new ArrayList<>()));
        }

        // 返回顶级节点（parentId = 0）
        return parentMap.getOrDefault(0L, new ArrayList<>());
    }
}
