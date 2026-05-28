package com.auction.business.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletTransactionExportRow {

    @ExcelProperty("流水号")
    private String transactionNo;

    @ExcelProperty("用户ID")
    private Long userId;

    @ExcelProperty("动作类型")
    private String actionType;

    @ExcelProperty("方向")
    private String direction;

    @ExcelProperty("金额")
    private BigDecimal amount;

    @ExcelProperty("变更前余额")
    private BigDecimal balanceBefore;

    @ExcelProperty("变更后余额")
    private BigDecimal balanceAfter;

    @ExcelProperty("变更前冻结")
    private BigDecimal frozenBefore;

    @ExcelProperty("变更后冻结")
    private BigDecimal frozenAfter;

    @ExcelProperty("业务类型")
    private String bizType;

    @ExcelProperty("业务ID")
    private String bizId;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建时间")
    private String createdAt;
}
