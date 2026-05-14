package com.auction.framework.web;

import com.auction.common.constant.TraceConstants;
import com.auction.common.core.Result;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一给接口响应补充 traceId。
 * 这样前端拿到错误响应时，可以把 traceId 提供给后端快速定位日志。
 */
@RestControllerAdvice
public class TraceIdResponseAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 返回 true 表示所有 Controller 响应都会进入 beforeBodyWrite。
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /**
     * 在响应写出前，如果返回值是 Result，就把当前请求的 traceId 放进去。
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof Result<?> result) {
            result.withTraceId(MDC.get(TraceConstants.TRACE_ID));
        }
        return body;
    }
}
