package com.auction.framework.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

/**
 * 安全工具类。
 * 封装从 Spring Security 上下文获取当前登录用户的常用操作，
 * 让 Service / Controller 不需要每次都写 SecurityContextHolder 样板代码。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户对象，未登录返回 null。
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取当前登录用户ID，未登录返回 null。
     */
    public static Long getUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    /**
     * 获取当前登录用户名，未登录返回 null。
     */
    public static String getUsername() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUsername() : null;
    }

    /**
     * 获取当前登录用户的角色编码列表，未登录返回空列表。
     */
    public static List<String> getRoles() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getRoles() : Collections.emptyList();
    }

    /**
     * 判断当前用户是否拥有指定角色。
     */
    public static boolean hasRole(String roleCode) {
        return getRoles().contains(roleCode);
    }

    /**
     * 判断当前用户是否为超级管理员。
     */
    public static boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }
}
