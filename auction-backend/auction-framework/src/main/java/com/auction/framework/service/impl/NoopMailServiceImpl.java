package com.auction.framework.service.impl;

import com.auction.framework.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 空实现的邮件服务（开发阶段占位）。
 * 仅在日志中输出发送请求，不真正发送邮件。
 * 后续接入 Spring Mail 后替换为真正的实现即可。
 */
@Slf4j
@Service
public class NoopMailServiceImpl implements MailService {

    @Override
    public void send(String to, String subject, String content) {
        log.info("[NOOP-MAIL] to={}, subject={}, content={}", to, subject, content);
    }

    @Override
    public void sendTemplate(String to, String subject, String templateName, Map<String, Object> model) {
        log.info("[NOOP-MAIL] to={}, subject={}, template={}, model={}", to, subject, templateName, model);
    }
}
