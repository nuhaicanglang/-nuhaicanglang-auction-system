package com.auction.common.exception;

import com.auction.common.core.ErrorCode;
import lombok.Getter;

/**
 * 业务异常。
 * 主动抛出 BizException 表示这是可预期的业务失败，例如用户不存在、余额不足。
 */
@Getter
public class BizException extends RuntimeException {

    /**
     * 业务错误码，最终会由全局异常处理器返回给前端。
     */
    private final Integer code;

    /**
     * 使用系统预定义错误码创建业务异常。
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    /**
     * 使用自定义错误码和提示信息创建业务异常。
     */
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
