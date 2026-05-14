package com.auction.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求参数。
 * DTO 专门接收前端提交的数据，和数据库实体 SysUser 分开可以避免前端传入不该修改的字段。
 */
@Data
public class SysUserRegisterDTO {

    /**
     * 登录用户名，只允许字母、数字和下划线，长度 4 到 20 位。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在4到20位之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /**
     * 登录密码，注册时必须传入，保存到数据库前会使用 BCrypt 加密。
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度必须在6到30位之间")
    private String password;

    /**
     * 用户昵称，用于页面展示。
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50位")
    private String nickname;

    /**
     * 邮箱，可用于后续登录、验证码、找回密码等功能。
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100位")
    private String email;

    /**
     * 手机号，当前按中国大陆 11 位手机号做基础校验。
     */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
