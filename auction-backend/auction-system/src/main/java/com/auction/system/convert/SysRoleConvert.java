package com.auction.system.convert;

import com.auction.system.entity.SysRole;
import com.auction.system.vo.SysRoleVO;

/**
 * 角色对象转换工具。
 */
public final class SysRoleConvert {

    private SysRoleConvert() {
    }

    public static SysRoleVO toVO(SysRole role) {
        if (role == null) {
            return null;
        }
        SysRoleVO vo = new SysRoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setDescription(role.getDescription());
        vo.setSortOrder(role.getSortOrder());
        vo.setStatus(role.getStatus());
        vo.setCreatedAt(role.getCreatedAt());
        return vo;
    }
}
