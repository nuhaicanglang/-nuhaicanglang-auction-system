package com.auction.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 全局线程池配置。
 * 四个线程池各司其职，互不抢占资源：
 * <ul>
 *   <li>logExecutor     — 操作日志异步入库</li>
 *   <li>notifyExecutor  — 通知消息（站内信/邮件）</li>
 *   <li>esSyncExecutor  — ES 数据同步</li>
 *   <li>statsExecutor   — 统计/导出</li>
 * </ul>
 * 拒绝策略统一用 CallerRunsPolicy（由调用线程执行），保证任务不丢失。
 */
@Configuration
public class ThreadPoolConfig {

    @Bean("logExecutor")
    public Executor logExecutor() {
        return buildExecutor("log-", 2, 4, 256);
    }

    @Bean("notifyExecutor")
    public Executor notifyExecutor() {
        return buildExecutor("notify-", 2, 8, 512);
    }

    @Bean("esSyncExecutor")
    public Executor esSyncExecutor() {
        return buildExecutor("es-sync-", 2, 4, 256);
    }

    @Bean("statsExecutor")
    public Executor statsExecutor() {
        return buildExecutor("stats-", 2, 4, 128);
    }

    /**
     * 统一构造线程池。
     */
    private Executor buildExecutor(String prefix, int core, int max, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix(prefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
