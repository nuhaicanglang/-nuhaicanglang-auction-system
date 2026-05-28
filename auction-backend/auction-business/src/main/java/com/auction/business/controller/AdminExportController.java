package com.auction.business.controller;

import com.auction.business.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/export")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminExportController {

    private final ExportService exportService;

    @GetMapping("/orders")
    public void exportOrders(HttpServletResponse response) throws IOException {
        exportService.exportOrders(response);
    }

    @GetMapping("/users")
    public void exportUsers(HttpServletResponse response) throws IOException {
        exportService.exportUsers(response);
    }

    @GetMapping("/wallet")
    public void exportWallet(HttpServletResponse response) throws IOException {
        exportService.exportWalletTransactions(response);
    }
}
