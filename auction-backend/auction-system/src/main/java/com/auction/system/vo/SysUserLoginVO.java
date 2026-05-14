package com.auction.system.vo;

import lombok.Data;

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
}
