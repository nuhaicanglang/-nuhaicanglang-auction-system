package com.auction.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品分类实体。
 * 使用 parent_id 自引用实现无限级树，path 字段冗余存储祖先路径（如 100/110/111），
 * 可以高效查询某节点的所有祖先。
 */
@Data
@TableName("biz_category")
public class BizCategory {

    @TableId
    private Long id;

    /** 父分类 ID，根节点为 0 */
    private Long parentId;

    /** 路径字符串，如 "100/110/111"，便于查询祖先链 */
    private String path;

    /** 层级：1/2/3 */
    private Integer level;

    private String name;

    private String icon;

    private String description;

    private Integer sortOrder;

    /** 1启用 / 0停用 */
    private Integer status;

    /** 在售商品数（冗余，定时刷新） */
    private Integer itemCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
