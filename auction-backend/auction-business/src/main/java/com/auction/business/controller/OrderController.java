package com.auction.business.controller;

import com.auction.business.dto.OrderQueryDTO;
import com.auction.business.service.OrderService;
import com.auction.business.service.PaymentService;
import com.auction.business.vo.OrderVO;
import com.auction.business.vo.PaymentVO;
import com.auction.common.core.Result;
import com.auction.framework.security.LoginUser;
import com.auction.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口。
 * 买家查看“我买到的”，卖家查看“我卖出的”，详情只允许订单双方查看。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    /** 买家视角：我买到的订单。 */
    @GetMapping("/buyer")
    public Result<IPage<OrderVO>> buyerOrders(OrderQueryDTO query) {
        LoginUser user = SecurityUtils.getLoginUser();
        return Result.success(orderService.listBuyerOrders(user.getUserId(), query));
    }

    /** 卖家视角：我卖出的订单。 */
    @GetMapping("/seller")
    public Result<IPage<OrderVO>> sellerOrders(OrderQueryDTO query) {
        LoginUser user = SecurityUtils.getLoginUser();
        return Result.success(orderService.listSellerOrders(user.getUserId(), query));
    }

    /** 订单详情。 */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        LoginUser user = SecurityUtils.getLoginUser();
        return Result.success(orderService.getOrderDetail(id, user.getUserId()));
    }

    /** 模拟钱包支付订单。 */
    @PostMapping("/{id}/pay")
    public Result<PaymentVO> pay(@PathVariable Long id, HttpServletRequest request) {
        LoginUser user = SecurityUtils.getLoginUser();
        String idempotentKey = request.getHeader("X-Idempotent-Key");
        return Result.success(paymentService.payOrder(id, user.getUserId(), idempotentKey));
    }

    /** 卖家发货（仅已支付状态可发货）。 */
    @PostMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id) {
        LoginUser user = SecurityUtils.getLoginUser();
        orderService.shipOrder(id, user.getUserId());
        return Result.success();
    }

    /** 买家确认完成订单（仅已支付/已发货状态可确认）。 */
    @PostMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        LoginUser user = SecurityUtils.getLoginUser();
        orderService.completeOrder(id, user.getUserId());
        return Result.success();
    }
}
