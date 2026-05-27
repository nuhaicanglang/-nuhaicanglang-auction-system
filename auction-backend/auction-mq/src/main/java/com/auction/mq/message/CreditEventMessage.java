package com.auction.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditEventMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType;
    private Long userId;
    private String relatedId;
}
