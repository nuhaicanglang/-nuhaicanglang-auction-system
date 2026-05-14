package com.auction.common.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 后端接口统一返回对象。
 * 所有 Controller 尽量返回这个结构，前端就可以用统一方式判断成功、失败和读取数据。
 *
 * @param <T> data 字段的实际数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务状态码，0 表示成功，非 0 表示失败。
     */
    private Integer code;

    /**
     * 给前端或调用方看的提示信息。
     */
    private String msg;

    /**
     * 接口真正返回的数据。
     */
    private T data;

    /**
     * 请求追踪ID，方便前端报错时和后端日志对应起来。
     */
    private String traceId;

    /**
     * 返回无数据的成功结果。
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 返回带业务数据的成功结果。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data, null);
    }

    /**
     * 根据统一错误码返回失败结果。
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMsg(), null, null);
    }

    /**
     * 返回自定义错误码和错误信息。
     */
    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null, null);
    }

    /**
     * 填充 traceId，并返回当前对象，方便链式调用。
     */
    public Result<T> withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }
}
