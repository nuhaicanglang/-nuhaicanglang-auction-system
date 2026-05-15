package com.auction.business.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出价记录视图对象。
 */
@Data
public class BidVO {

    private Long id;
    private Long itemId;
    private Long bidderId;
    /** 脱敏后的出价人名（如 t***5） */
    private String bidderName;
    private BigDecimal bidPrice;
    private LocalDateTime bidTime;
    private Integer bidType;
    private Integer status;
}
