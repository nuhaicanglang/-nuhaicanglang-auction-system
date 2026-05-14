package com.auction.common.constant;

/**
 * 链路追踪相关常量。
 */
public final class TraceConstants {

    /**
     * 日志 MDC 中保存 traceId 使用的 key。
     */
    public static final String TRACE_ID = "traceId";

    /**
     * HTTP 请求头和响应头中传递 traceId 使用的名称。
     */
    public static final String TRACE_HEADER = "X-Trace-Id";

    /**
     * 工具类不需要被实例化。
     */
    private TraceConstants() {
    }
}
