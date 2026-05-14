package com.auction.system.service.impl;

import com.auction.common.core.ErrorCode;
import com.auction.common.exception.BizException;
import com.auction.common.util.SnowflakeIdWorker;
import com.auction.system.convert.SysUserConvert;
import com.auction.system.dto.SysUserLoginDTO;
import com.auction.system.dto.SysUserRegisterDTO;
import com.auction.system.entity.SysUser;
import com.auction.system.mapper.SysUserMapper;
import com.auction.system.security.JwtTokenProvider;
import com.auction.system.service.SysUserService;
import com.auction.system.vo.SysUserLoginVO;
import com.auction.system.vo.SysUserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户业务实现类。
 * ServiceImpl 已经封装了 getById、save、updateById 等基础数据库操作。
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    private final SnowflakeIdWorker snowflakeIdWorker = new SnowflakeIdWorker();

    /**
     * 注册新用户。
     * 这里集中处理唯一性校验、密码加密、ID 生成和数据库保存。
     */
    @Override
    public SysUserVO register(SysUserRegisterDTO registerDTO) {
        String email = normalizeBlank(registerDTO.getEmail());
        String phone = normalizeBlank(registerDTO.getPhone());

        checkUnique(registerDTO.getUsername(), email, phone);

        SysUser user = new SysUser();
        user.setId(snowflakeIdWorker.nextId());
        user.setUsername(registerDTO.getUsername());
        user.setNickname(registerDTO.getNickname());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(email);
        user.setPhone(phone);
        user.setGender(0);
        user.setStatus(1);
        user.setTenantId(0L);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);

        save(user);
        return SysUserConvert.toVO(user);
    }

    /**
     * 用户登录。
     * 先按用户名查询用户，再用 BCrypt 校验明文密码和数据库密文是否匹配。
     */
    @Override
    public SysUserLoginVO login(SysUserLoginDTO loginDTO) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginDTO.getUsername())
                .last("limit 1"));

        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        if (Integer.valueOf(2).equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_BLACKLISTED);
        }

        user.setLastLoginAt(LocalDateTime.now());
        updateById(user);

        SysUserLoginVO loginVO = new SysUserLoginVO();
        loginVO.setToken(jwtTokenProvider.createToken(user.getId(), user.getUsername()));
        loginVO.setUser(SysUserConvert.toVO(user));
        return loginVO;
    }

    /**
     * 校验用户名、邮箱、手机号是否已经被正常用户占用。
     */
    private void checkUnique(String username, String email, String phone) {
        if (count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        if (StringUtils.hasText(email)
                && count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            throw new BizException(ErrorCode.EMAIL_EXISTS);
        }
        if (StringUtils.hasText(phone)
                && count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone)) > 0) {
            throw new BizException(ErrorCode.PHONE_EXISTS);
        }
    }

    /**
     * 把空字符串统一转成 null，避免数据库唯一索引把多个空邮箱或空手机号当成重复值。
     */
    private String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

}
