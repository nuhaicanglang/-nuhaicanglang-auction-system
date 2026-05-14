package com.auction.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户返回对象。
 * VO 面向前端返回数据，所以不会包含 password、idCardNo 等敏感字段。
 */
@Data
public class SysUserVO {

    /**
     * 用户ID。
     */
    private Long id;

    /**
     * 登录用户名。
     */
    private String username;

    /**
     * 用户昵称。
     */
    private String nickname;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 头像URL。
     */
    private String avatar;

    /**
     * 性别：0未知，1男，2女。
     */
    private Integer gender;

    /**
     * 用户状态：1正常，0禁用，2黑名单。
     */
    private Integer status;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
