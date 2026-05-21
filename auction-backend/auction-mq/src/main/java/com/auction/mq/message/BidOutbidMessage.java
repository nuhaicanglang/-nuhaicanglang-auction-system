package com.auction.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidOutbidMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long itemId;
    private String itemTitle;
    private Long outbidUserId;
    private Long newBidderId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private Long bidId;
}
