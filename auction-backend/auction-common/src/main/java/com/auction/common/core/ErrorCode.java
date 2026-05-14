package com.auction.common.core;

import lombok.Getter;

/**
 * 系统统一错误码。
 * 错误码分段可以让前端、日志和后端快速判断问题属于哪个业务域。
 */
@Getter
public enum ErrorCode {

    /**
     * 通用成功。
     */
    SUCCESS(0, "success"),

    /**
     * 100xx：通用和用户认证相关错误。
     */
    PARAM_ERROR(10001, "参数错误"),
    UNAUTHORIZED(10002, "未登录或登录已过期"),
    FORBIDDEN(10003, "无权限访问"),
    USER_NOT_FOUND(10004, "用户不存在"),
    PASSWORD_ERROR(10005, "账号或密码错误"),
    USERNAME_EXISTS(10006, "用户名已存在"),
    EMAIL_EXISTS(10007, "邮箱已被使用"),
    PHONE_EXISTS(10008, "手机号已被使用"),
    USER_DISABLED(10009, "账号已被禁用"),
    USER_BLACKLISTED(10010, "账号已被拉黑"),

    /**
     * 300xx：拍品相关错误。
     */
    ITEM_NOT_FOUND(30003, "商品不存在"),
    ITEM_STATUS_NOT_ALLOWED(30004, "商品状态不允许此操作"),

    /**
     * 400xx：出价相关错误。
     */
    BID_PRICE_TOO_LOW(40001, "出价金额不足"),
    BID_SELF_FORBIDDEN(40002, "不能给自己的商品出价"),
    BID_AUCTION_NOT_RUNNING(40003, "拍卖未开始或已结束"),
    BID_TOO_FREQUENT(40004, "出价过于频繁"),
    BID_DEPOSIT_NOT_ENOUGH(40005, "保证金不足"),
    BID_CREDIT_NOT_ENOUGH(40006, "信用分不足,无法出价"),
    BID_OUTDATED(40007, "出价已被超过,请刷新重试"),
    BID_BALANCE_NOT_ENOUGH(40008, "资金不足,联系管理员充值"),
    BID_BLACKLISTED(40009, "账号已被拉黑,不能出价"),

    /**
     * 500xx：订单、钱包、验证码等业务错误。
     */
    ORDER_NOT_FOUND(50001, "订单不存在"),
    WALLET_BALANCE_NOT_ENOUGH(50004, "余额不足"),
    EMAIL_CODE_ERROR(50101, "邮箱验证码错误或已过期"),
    CAPTCHA_ERROR(50103, "图形验证码错误"),

    /**
     * 900xx 和 99999：系统级错误。
     */
    SYSTEM_LIMITED(90001, "系统限流"),
    SYSTEM_ERROR(99999, "系统异常");

    /**
     * 数字错误码，便于前端按 code 做分支处理。
     */
    private final Integer code;

    /**
     * 默认错误提示。
     */
    private final String msg;

    ErrorCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
