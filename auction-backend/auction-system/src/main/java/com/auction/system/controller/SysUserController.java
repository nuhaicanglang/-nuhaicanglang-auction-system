package com.auction.system.controller;

import com.auction.common.core.ErrorCode;
import com.auction.common.core.Result;
import com.auction.common.exception.BizException;
import com.auction.framework.security.CaptchaService;
import com.auction.framework.security.JwtBlacklistService;
import com.auction.framework.security.JwtTokenProvider;
import com.auction.framework.security.LoginUser;
import com.auction.system.convert.SysUserConvert;
import com.auction.system.dto.SysUserLoginDTO;
import com.auction.system.dto.SysUserRegisterDTO;
import com.auction.system.entity.SysRole;
import com.auction.system.entity.SysUser;
import com.auction.system.mapper.SysRoleMapper;
import com.auction.system.service.SysUserService;
import com.auction.system.vo.SysUserLoginVO;
import com.auction.system.vo.SysUserVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理测试接口。
 * 当前阶段主要用于验证 Controller -> Service -> Mapper -> MySQL 的完整调用链路。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/users")
public class SysUserController {

    private final SysUserService sysUserService;

    private final CaptchaService captchaService;

    private final JwtTokenProvider jwtTokenProvider;

    private final JwtBlacklistService jwtBlacklistService;

    private final SysRoleMapper sysRoleMapper;

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

    /**
     * 获取图形验证码。
     * 返回 uuid 和 base64 图片，前端登录时携带 uuid + 用户输入的答案。
     */
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        return Result.success(captchaService.generate());
    }

    /**
     * 登出接口。
     * 将当前 Access Token 加入 Redis 黑名单，剩余有效期内不再可用。
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Date expiration = jwtTokenProvider.getExpiration(token);
                long remainingMs = expiration.getTime() - System.currentTimeMillis();
                jwtBlacklistService.addToBlacklist(token, remainingMs);
            } catch (JwtException ignored) {
                // token 已过期或无效，无需加黑名单
            }
        }
        return Result.success(null);
    }

    /**
     * 刷新 Token 接口（静默续签）。
     * 前端在 Access Token 过期时，用 Refresh Token 换取新的 Access Token 和 Refresh Token。
     */
    @PostMapping("/refresh")
    public Result<SysUserLoginVO> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (!StringUtils.hasText(refreshToken)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        try {
            Claims claims = jwtTokenProvider.parseClaims(refreshToken);
            String type = claims.get("type", String.class);
            if (!"refresh".equals(type)) {
                throw new BizException(ErrorCode.UNAUTHORIZED);
            }
            Long userId = Long.valueOf(claims.getSubject());
            SysUser user = sysUserService.getById(userId);
            if (user == null) {
                throw new BizException(ErrorCode.USER_NOT_FOUND);
            }
            if (Integer.valueOf(0).equals(user.getStatus())) {
                throw new BizException(ErrorCode.USER_DISABLED);
            }
            if (Integer.valueOf(2).equals(user.getStatus())) {
                throw new BizException(ErrorCode.USER_BLACKLISTED);
            }

            List<String> roleCodes = sysRoleMapper.selectRolesByUserId(userId)
                    .stream().map(SysRole::getCode).collect(Collectors.toList());

            SysUserLoginVO loginVO = new SysUserLoginVO();
            loginVO.setToken(jwtTokenProvider.createToken(userId, user.getUsername(), roleCodes));
            loginVO.setRefreshToken(jwtTokenProvider.createRefreshToken(userId));
            loginVO.setUser(SysUserConvert.toVO(user));
            loginVO.setRoles(roleCodes);
            return Result.success(loginVO);
        } catch (JwtException e) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }
}
