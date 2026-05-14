package com.auction.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类，对应数据库表 sys_user。
 * Entity 主要负责和数据库字段一一映射，通常不直接返回给前端。
 */
@Data
@TableName("sys_user")
public class SysUser {

    /**
     * 主键ID，由雪花算法生成，数据库不自增。
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 登录用户名，系统内唯一。
     */
    private String username;

    /**
     * 用户展示昵称。
     */
    private String nickname;

    /**
     * BCrypt 加密后的密码，不能返回给前端。
     */
    private String password;

    /**
     * 邮箱，后续可用于验证码、找回密码等功能。
     */
    private String email;

    /**
     * 手机号，预留给短信验证码、实名认证等功能。
     */
    private String phone;

    /**
     * 用户头像URL。
     */
    private String avatar;

    /**
     * 性别：0未知，1男，2女。
     */
    private Integer gender;

    /**
     * 实名认证姓名。
     */
    private String realName;

    /**
     * 身份证号，实际生产环境应加密存储或脱敏展示。
     */
    private String idCardNo;

    /**
     * 用户状态：1正常，0禁用，2黑名单。
     */
    private Integer status;

    /**
     * 拉黑原因，仅当 status=2 时有值。
     */
    private String blacklistReason;

    /**
     * 执行拉黑操作的管理员ID。
     */
    private Long blacklistedBy;

    /**
     * 拉黑时间。
     */
    private LocalDateTime blacklistedAt;

    /**
     * 最后登录时间。
     */
    private LocalDateTime lastLoginAt;

    /**
     * 最后登录IP。
     */
    private String lastLoginIp;

    /**
     * 租户ID，当前单体系统暂时使用默认值0。
     */
    private Long tenantId;

    /**
     * 创建人ID。
     */
    private Long createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新人ID。
     */
    private Long updatedBy;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除字段：0未删除，1已删除。
     */
    @TableLogic
    private Integer deleted;
}
