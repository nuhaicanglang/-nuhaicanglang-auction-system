package com.auction.business.controller;

import com.auction.business.dto.OrderQueryDTO;
import com.auction.business.service.OrderService;
import com.auction.business.vo.OrderVO;
import com.auction.common.core.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端订单接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<IPage<OrderVO>> list(OrderQueryDTO query) {
        return Result.success(orderService.listAdminOrders(query));
    }
}
