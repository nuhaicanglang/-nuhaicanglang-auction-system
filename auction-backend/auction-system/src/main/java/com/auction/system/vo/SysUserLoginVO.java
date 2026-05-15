package com.auction.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户登录成功后的返回对象。
 * token 用于证明用户已经登录，user 用于前端展示当前用户基础信息。
 */
@Data
public class SysUserLoginVO {

    /**
     * JWT 登录凭证。
     */
    private String token;

    /**
     * 登录用户的安全展示信息，不包含密码等敏感字段。
     */
    private SysUserVO user;

    /**
     * 用户拥有的角色编码列表，前端可据此控制菜单显隐和按钮权限。
     */
    private List<String> roles;

    /**
     * Refresh Token，用于在 Access Token 过期后静默续签。
     */
    private String refreshToken;
}
