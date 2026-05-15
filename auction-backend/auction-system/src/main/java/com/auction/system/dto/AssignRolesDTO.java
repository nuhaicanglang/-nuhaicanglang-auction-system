package com.auction.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 分配角色请求体。
 * 管理员通过此 DTO 为指定用户设置角色列表。
 */
@Data
public class AssignRolesDTO {

    /**
     * 要分配的角色ID列表，传空列表表示清除所有角色。
     */
    @NotNull(message = "角色列表不能为空")
    private List<Long> roleIds;
}
