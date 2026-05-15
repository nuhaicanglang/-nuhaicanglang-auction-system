package com.auction.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求参数。
 * 登录时只需要账号和密码，不应该复用注册 DTO，避免无关字段参与校验。
 */
@Data
public class SysUserLoginDTO {

    /**
     * 登录用户名。
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 登录密码，后端会和数据库中的 BCrypt 密文进行匹配。
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证码 UUID（连续登录失败 >= 3 次后必传）。
     */
    private String captchaUuid;

    /**
     * 验证码答案（连续登录失败 >= 3 次后必传）。
     */
    private String captchaCode;
}
