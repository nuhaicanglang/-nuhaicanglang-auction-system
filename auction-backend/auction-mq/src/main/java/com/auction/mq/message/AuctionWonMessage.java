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
public class AuctionWonMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long itemId;
    private String itemTitle;
    private Long winnerId;
    private BigDecimal finalPrice;
    private Long bidId;
}
