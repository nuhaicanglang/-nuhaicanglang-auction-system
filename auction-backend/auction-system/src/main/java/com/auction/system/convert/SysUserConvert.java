package com.auction.system.convert;

import com.auction.system.entity.SysUser;
import com.auction.system.vo.SysUserVO;

/**
 * 用户对象转换工具。
 * 统一管理 Entity 到 VO 的转换逻辑，避免 Controller 和 Service 中重复编写字段复制代码。
 */
public final class SysUserConvert {

    private SysUserConvert() {
    }

    /**
     * 将数据库用户实体转换为前端安全展示对象。
     */
    public static SysUserVO toVO(SysUser user) {
        if (user == null) {
            return null;
        }

        SysUserVO vo = new SysUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
