package com.auction.framework.web;

import com.auction.common.constant.TraceConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求追踪过滤器。
 * 每个请求进入系统时都会拥有一个 traceId，后续日志和接口响应都可以用它串起来。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * 为当前 HTTP 请求准备 traceId，并在请求结束后清理线程上下文。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TraceConstants.TRACE_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(TraceConstants.TRACE_ID, traceId);
        response.setHeader(TraceConstants.TRACE_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Web 容器线程会复用，请求结束必须清理，避免 traceId 串到下一个请求。
            MDC.remove(TraceConstants.TRACE_ID);
        }
    }
}
