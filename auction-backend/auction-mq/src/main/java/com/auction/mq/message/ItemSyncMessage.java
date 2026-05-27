package com.auction.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ES 商品同步消息。
 * 当商品新增/修改/删除时，通过 MQ 发送此消息触发 ES 增量同步。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemSyncMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private Long itemId;

    /** 操作类型：UPSERT / DELETE */
    private String action;
}
