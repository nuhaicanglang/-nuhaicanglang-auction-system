package com.auction.business.job;

import com.auction.business.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyCreditRestoreJob {

    private final CreditService creditService;

    @Scheduled(cron = "0 0 2 1 * ?")
    public void restore() {
        log.info("开始执行每月信用分恢复任务");
        creditService.restoreMonthlyCredit();
    }
}
