package com.auction.framework.web;

import com.auction.common.core.ErrorCode;
import com.auction.common.core.Result;
import com.auction.common.exception.BizException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.auction.common.constant.TraceConstants.TRACE_ID;

/**
 * 全局异常处理器。
 * 统一把异常转换成 Result，避免接口直接把 Java 异常堆栈暴露给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常，例如用户不存在、余额不足、商品状态不允许操作。
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        return Result.<Void>fail(e.getCode(), e.getMessage()).withTraceId(MDC.get(TRACE_ID));
    }

    /**
     * 处理参数校验异常，统一返回“参数错误”。
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public Result<Void> handleValidationException(Exception e) {
        return Result.<Void>fail(ErrorCode.PARAM_ERROR).withTraceId(MDC.get(TRACE_ID));
    }

    /**
     * 处理未预料到的系统异常，需要记录完整日志，返回给前端时只给通用提示。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return Result.<Void>fail(ErrorCode.SYSTEM_ERROR).withTraceId(MDC.get(TRACE_ID));
    }
}
