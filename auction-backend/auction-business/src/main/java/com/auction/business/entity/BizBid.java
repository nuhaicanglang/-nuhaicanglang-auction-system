package com.auction.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出价记录实体。
 * 对应 biz_bid 表，记录每一次有效/被超/撤销的出价。
 */
@Data
@TableName("biz_bid")
public class BizBid {

    @TableId
    private Long id;

    private Long itemId;
    private Long bidderId;
    private BigDecimal bidPrice;
    private LocalDateTime bidTime;

    /** 1正常/2自动/3一口价 */
    private Integer bidType;

    /** 1有效/2已被超/3已撤销 */
    private Integer status;

    private String clientIp;
    private String clientRequestId;
    private Long tenantId;
    private LocalDateTime createdAt;
}
