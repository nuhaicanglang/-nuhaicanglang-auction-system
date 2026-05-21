package com.auction.business.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotifyCreateDTO {

    private Long userId;
    private Integer type;
    private String title;
    private String content;
    private Long relatedItemId;
}
