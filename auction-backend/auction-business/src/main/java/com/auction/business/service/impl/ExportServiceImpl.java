package com.auction.business.service.impl;

import com.alibaba.excel.EasyExcel;
import com.auction.business.entity.BizOrder;
import com.auction.business.entity.BizWalletTransaction;
import com.auction.business.excel.OrderExportRow;
import com.auction.business.excel.UserExportRow;
import com.auction.business.excel.WalletTransactionExportRow;
import com.auction.business.mapper.BizOrderMapper;
import com.auction.business.mapper.BizWalletTransactionMapper;
import com.auction.business.service.ExportService;
import com.auction.system.entity.SysUser;
import com.auction.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private static final int MAX_EXPORT_ROWS = 5000;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BizOrderMapper orderMapper;
    private final SysUserMapper userMapper;
    private final BizWalletTransactionMapper walletTransactionMapper;

    @Override
    public void exportOrders(HttpServletResponse response) throws IOException {
        List<BizOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<BizOrder>()
                .orderByDesc(BizOrder::getCreatedAt)
                .last("LIMIT " + MAX_EXPORT_ROWS));
        List<OrderExportRow> rows = orders.stream().map(this::toOrderRow).collect(Collectors.toList());
        writeExcel(response, "orders", OrderExportRow.class, rows);
    }

    @Override
    public void exportUsers(HttpServletResponse response) throws IOException {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
                .orderByDesc(SysUser::getCreatedAt)
                .last("LIMIT " + MAX_EXPORT_ROWS));
        List<UserExportRow> rows = users.stream().map(this::toUserRow).collect(Collectors.toList());
        writeExcel(response, "users", UserExportRow.class, rows);
    }

    @Override
    public void exportWalletTransactions(HttpServletResponse response) throws IOException {
        List<BizWalletTransaction> txs = walletTransactionMapper.selectList(new LambdaQueryWrapper<BizWalletTransaction>()
                .orderByDesc(BizWalletTransaction::getCreatedAt)
                .last("LIMIT " + MAX_EXPORT_ROWS));
        List<WalletTransactionExportRow> rows = txs.stream().map(this::toWalletRow).collect(Collectors.toList());
        writeExcel(response, "wallet", WalletTransactionExportRow.class, rows);
    }

    private <T> void writeExcel(HttpServletResponse response, String fileName,
                                Class<T> headClass, List<T> rows) throws IOException {
        String encoded = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        EasyExcel.write(response.getOutputStream(), headClass)
                .sheet("data")
                .doWrite(rows);
    }

    private OrderExportRow toOrderRow(BizOrder o) {
        OrderExportRow row = new OrderExportRow();
        row.setOrderNo(o.getOrderNo());
        row.setItemId(o.getItemId());
        row.setItemTitle(o.getItemTitle());
        row.setBuyerId(o.getBuyerId());
        row.setSellerId(o.getSellerId());
        row.setDealPrice(o.getDealPrice());
        row.setDepositAmount(o.getDepositAmount());
        row.setPayAmount(o.getPayAmount());
        row.setStatus(orderStatus(o.getStatus()));
        row.setPayDeadline(format(o.getPayDeadline()));
        row.setPaidAt(format(o.getPaidAt()));
        row.setCreatedAt(format(o.getCreatedAt()));
        return row;
    }

    private UserExportRow toUserRow(SysUser u) {
        UserExportRow row = new UserExportRow();
        row.setId(u.getId());
        row.setUsername(u.getUsername());
        row.setNickname(u.getNickname());
        row.setEmail(u.getEmail());
        row.setPhone(u.getPhone());
        row.setStatus(userStatus(u.getStatus()));
        row.setLastLoginAt(format(u.getLastLoginAt()));
        row.setCreatedAt(format(u.getCreatedAt()));
        return row;
    }

    private WalletTransactionExportRow toWalletRow(BizWalletTransaction t) {
        WalletTransactionExportRow row = new WalletTransactionExportRow();
        row.setTransactionNo(t.getTransactionNo());
        row.setUserId(t.getUserId());
        row.setActionType(t.getActionType());
        row.setDirection(t.getDirection() != null && t.getDirection() == 1 ? "收入" : "支出");
        row.setAmount(t.getAmount());
        row.setBalanceBefore(t.getBalanceBefore());
        row.setBalanceAfter(t.getBalanceAfter());
        row.setFrozenBefore(t.getFrozenBefore());
        row.setFrozenAfter(t.getFrozenAfter());
        row.setBizType(t.getBizType());
        row.setBizId(t.getBizId());
        row.setRemark(t.getRemark());
        row.setCreatedAt(format(t.getCreatedAt()));
        return row;
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : FMT.format(time);
    }

    private String orderStatus(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "待支付";
            case 2 -> "已支付";
            case 3 -> "已发货";
            case 4 -> "已完成";
            case 5 -> "已取消";
            case 6 -> "已关闭";
            default -> "未知";
        };
    }

    private String userStatus(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "禁用";
            case 1 -> "正常";
            case 2 -> "黑名单";
            default -> "未知";
        };
    }
}
