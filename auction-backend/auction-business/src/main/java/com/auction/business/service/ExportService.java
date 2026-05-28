package com.auction.business.service;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface ExportService {

    void exportOrders(HttpServletResponse response) throws IOException;

    void exportUsers(HttpServletResponse response) throws IOException;

    void exportWalletTransactions(HttpServletResponse response) throws IOException;
}
