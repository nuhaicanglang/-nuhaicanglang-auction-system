package com.auction.system.controller;

import com.auction.common.core.ErrorCode;
import com.auction.common.core.Result;
import com.auction.common.exception.BizException;
import com.auction.framework.security.LoginUser;
import com.auction.system.convert.SysUserConvert;
import com.auction.system.dto.SysUserLoginDTO;
import com.auction.system.dto.SysUserRegisterDTO;
import com.auction.system.entity.SysUser;
import com.auction.system.service.SysUserService;
import com.auction.system.vo.SysUserLoginVO;
import com.auction.system.vo.SysUserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理测试接口。
 * 当前阶段主要用于验证 Controller -> Service -> Mapper -> MySQL 的完整调用链路。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/users")
public class SysUserController {

    private final SysUserService sysUserService;

    /**
     * 用户注册接口。
     * RequestBody 表示从 JSON 请求体中读取注册参数，Valid 会触发 DTO 上的参数校验注解。
     */
    @PostMapping("/register")
    public Result<SysUserVO> register(@Valid @RequestBody SysUserRegisterDTO registerDTO) {
        return Result.success(sysUserService.register(registerDTO));
    }

    /**
     * 用户登录接口。
     * 登录成功后返回 JWT token 和用户基础信息，前端后续可携带 token 访问需要登录的接口。
     */
    @PostMapping("/login")
    public Result<SysUserLoginVO> login(@Valid @RequestBody SysUserLoginDTO loginDTO) {
        return Result.success(sysUserService.login(loginDTO));
    }

    /**
     * 查询当前登录用户信息。
     * AuthenticationPrincipal 会从 Spring Security 上下文中取出 JwtAuthFilter 放入的 LoginUser。
     */
    @GetMapping("/me")
    public Result<SysUserVO> getCurrentUser(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = sysUserService.getById(loginUser.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        SysUserVO vo = SysUserConvert.toVO(user);
        vo.setRoles(loginUser.getRoles());
        return Result.success(vo);
    }

    /**
     * 根据用户ID查询用户详情。
     */
    @GetMapping("/{id}")
    public Result<SysUserVO> getUserById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return Result.success(SysUserConvert.toVO(user));
    }
}
