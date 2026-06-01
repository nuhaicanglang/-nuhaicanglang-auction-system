package com.auction.system.service.impl;

import com.auction.framework.security.AccountStatusChecker;
import com.auction.system.entity.SysUser;
import com.auction.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Keeps already-issued JWTs tied to the current account status.
 */
@Component
@RequiredArgsConstructor
public class SysUserAccountStatusChecker implements AccountStatusChecker {

    private final SysUserMapper sysUserMapper;

    @Override
    public boolean isActive(Long userId) {
        if (userId == null) {
            return false;
        }
        SysUser user = sysUserMapper.selectById(userId);
        return user != null
                && Integer.valueOf(1).equals(user.getStatus())
                && (user.getDeleted() == null || user.getDeleted() == 0);
    }
}
