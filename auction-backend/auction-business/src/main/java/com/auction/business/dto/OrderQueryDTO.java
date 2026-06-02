package com.auction.business.dto;

import lombok.Data;

/**
 * 订单列表查询参数。
 */
@Data
public class OrderQueryDTO {

    /** 订单状态筛选：1待支付/2已支付/3已发货/4已完成/5已取消/6已关闭 */
    private Integer status;

    /** 后台订单号筛选 */
    private String orderNo;

    /** 后台按拍品标题搜索 */
    private String keyword;

    /** 后台买家筛选 */
    private Long buyerId;

    /** 后台卖家筛选 */
    private Long sellerId;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 20;
}
