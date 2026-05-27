package com.auction.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_credit_log")
public class BizCreditLog {

    @TableId
    private Long id;

    private Long userId;
    private String eventType;
    private String relatedId;
    private Integer deltaScore;
    private Integer scoreBefore;
    private Integer scoreAfter;
    private String remark;
    private String idempotentKey;
    private Long tenantId;
    private LocalDateTime createdAt;
}
