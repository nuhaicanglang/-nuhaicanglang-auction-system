package com.auction.framework.aspect;

/**
 * 操作日志持久化接口。
 * framework 层定义接口，system 层提供实现（写入 sys_oper_log 表），
 * 实现依赖倒置，避免 framework 反向依赖 system。
 */
public interface OperLogHandler {

    /**
     * 保存操作日志。
     */
    void save(OperLogEvent event);
}
