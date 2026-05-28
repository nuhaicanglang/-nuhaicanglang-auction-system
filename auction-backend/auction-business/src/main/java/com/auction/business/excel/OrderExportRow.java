package com.auction.business.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderExportRow {

    @ExcelProperty("订单号")
    private String orderNo;

    @ExcelProperty("商品ID")
    private Long itemId;

    @ExcelProperty("商品标题")
    private String itemTitle;

    @ExcelProperty("买家ID")
    private Long buyerId;

    @ExcelProperty("卖家ID")
    private Long sellerId;

    @ExcelProperty("成交价")
    private BigDecimal dealPrice;

    @ExcelProperty("保证金抵扣")
    private BigDecimal depositAmount;

    @ExcelProperty("应付金额")
    private BigDecimal payAmount;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("支付截止时间")
    private String payDeadline;

    @ExcelProperty("支付时间")
    private String paidAt;

    @ExcelProperty("创建时间")
    private String createdAt;
}
