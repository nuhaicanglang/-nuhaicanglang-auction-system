package com.auction.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色返回对象，用于前端展示角色列表和详情。
 */
@Data
public class SysRoleVO {

    private Long id;

    private String code;

    private String name;

    private String description;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createdAt;
}
