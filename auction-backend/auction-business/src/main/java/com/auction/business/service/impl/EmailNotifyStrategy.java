package com.auction.business.service.impl;

import com.auction.business.dto.NotifyCreateDTO;
import com.auction.business.service.NotifyChannelStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotifyStrategy implements NotifyChannelStrategy {

    @Override
    public String channel() {
        return "EMAIL";
    }

    @Override
    public void send(NotifyCreateDTO dto) {
        log.info("邮件通知占位: userId={}, title={}", dto.getUserId(), dto.getTitle());
    }
}
