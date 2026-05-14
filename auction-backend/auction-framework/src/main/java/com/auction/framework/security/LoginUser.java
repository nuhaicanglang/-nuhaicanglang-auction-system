package com.auction.framework.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户对象。
 * JWT 过滤器认证成功后会把它放入 Spring Security 上下文，Controller 可直接读取当前用户ID。
 */
@Data
@AllArgsConstructor
public class LoginUser {

    /**
     * 当前登录用户ID。
     */
    private Long userId;

    /**
     * 当前登录用户名。
     */
    private String username;
}
