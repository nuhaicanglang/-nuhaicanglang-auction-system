package com.auction.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户收藏拍品关系。
 */
@Data
@TableName("biz_favorite")
public class BizFavorite {

    @TableId
    private Long id;

    private Long userId;
    private Long itemId;
    private Long tenantId;
    private LocalDateTime createdAt;
}
