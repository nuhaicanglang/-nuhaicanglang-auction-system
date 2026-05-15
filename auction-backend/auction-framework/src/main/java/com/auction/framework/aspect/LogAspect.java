package com.auction.framework.aspect;

import com.auction.common.constant.TraceConstants;
import com.auction.framework.annotation.Log;
import com.auction.framework.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

/**
 * 操作日志切面。
 * 环绕通知拦截标注了 @Log 的 Controller 方法，采集请求和响应信息，
 * 通过 logExecutor 线程池异步交给 OperLogHandler 持久化，避免影响主流程性能。
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    private final OperLogHandler operLogHandler;
    private final ObjectMapper objectMapper;
    private final Executor logExecutor;

    public LogAspect(OperLogHandler operLogHandler,
                     ObjectMapper objectMapper,
                     @Qualifier("logExecutor") Executor logExecutor) {
        this.operLogHandler = operLogHandler;
        this.objectMapper = objectMapper;
        this.logExecutor = logExecutor;
    }

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Log logAnnotation) throws Throwable {
        long startMs = System.currentTimeMillis();
        OperLogEvent event = new OperLogEvent();
        event.setTraceId(MDC.get(TraceConstants.TRACE_ID));
        event.setModule(logAnnotation.module());
        event.setBusinessType(logAnnotation.businessType());
        event.setDescription(logAnnotation.description());

        // 方法签名
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        event.setMethod(ms.getDeclaringType().getSimpleName() + "." + ms.getName());

        // 请求信息
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            event.setRequestUrl(request.getRequestURI());
            event.setRequestMethod(request.getMethod());
            event.setOperIp(request.getRemoteAddr());
            event.setUserAgent(request.getHeader("User-Agent"));
        }

        // 当前用户
        event.setOperUserId(SecurityUtils.getUserId());
        event.setOperUserName(SecurityUtils.getUsername());

        // 请求参数（限长，避免 TEXT 字段过大）
        try {
            String params = objectMapper.writeValueAsString(joinPoint.getArgs());
            event.setRequestParams(truncate(params, 2000));
        } catch (Exception ignored) {
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            event.setStatus(0);
            // 响应数据（限长）
            try {
                event.setResponseData(truncate(objectMapper.writeValueAsString(result), 2000));
            } catch (Exception ignored) {
            }
        } catch (Throwable ex) {
            event.setStatus(1);
            event.setErrorMsg(truncate(ex.getMessage(), 2000));
            throw ex;
        } finally {
            event.setCostMs((int) (System.currentTimeMillis() - startMs));
            event.setCreatedAt(LocalDateTime.now());
            // 异步入库，不阻塞主线程
            logExecutor.execute(() -> {
                try {
                    operLogHandler.save(event);
                } catch (Exception e) {
                    log.error("保存操作日志失败", e);
                }
            });
        }
        return result;
    }

    private String truncate(String s, int maxLen) {
        return (s != null && s.length() > maxLen) ? s.substring(0, maxLen) : s;
    }
}
