package com.auction.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_credit")
public class BizCredit {

    @TableId
    private Long id;

    private Long userId;
    private Integer score;
    private String levelName;
    private Integer status;
    private LocalDateTime lastEventAt;
    private Long tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
