package com.auction.framework.service;

/**
 * 邮件服务接口（扩展点）。
 * 当前阶段仅定义接口，不实现真正的邮件发送。
 * 后续可通过 Spring Mail + Thymeleaf 模板实现，只需提供此接口的实现类即可。
 */
public interface MailService {

    /**
     * 发送简单文本邮件。
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件正文
     */
    void send(String to, String subject, String content);

    /**
     * 发送模板邮件。
     *
     * @param to           收件人邮箱
     * @param subject      邮件主题
     * @param templateName 模板名称（如 "verify-code"）
     * @param model        模板变量
     */
    void sendTemplate(String to, String subject, String templateName, java.util.Map<String, Object> model);
}
