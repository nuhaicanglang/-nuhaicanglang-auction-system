package com.auction.system.service;

import com.auction.system.dto.SysUserLoginDTO;
import com.auction.system.dto.SysUserRegisterDTO;
import com.auction.system.entity.SysUser;
import com.auction.system.vo.SysUserLoginVO;
import com.auction.system.vo.SysUserVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户业务接口。
 * 后续注册、登录、修改资料、禁用用户等业务逻辑会从这里扩展。
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 注册新用户。
     *
     * @param registerDTO 前端提交的注册信息
     * @return 注册成功后返回给前端的用户信息
     */
    SysUserVO register(SysUserRegisterDTO registerDTO);

    /**
     * 用户登录。
     *
     * @param loginDTO 前端提交的用户名和密码
     * @return 登录成功后的用户信息
     */
    SysUserLoginVO login(SysUserLoginDTO loginDTO);
}
