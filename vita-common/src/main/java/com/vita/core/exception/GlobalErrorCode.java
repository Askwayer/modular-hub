package com.vita.core.exception;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.exception
 * @Author: znk
 * @CreateTime: 2026-03-04  21:17:21
 * @Description: 全局错误码
 * @Version: 1.0
 */
public interface GlobalErrorCode {
    ErrorCode SUCCESS = new ErrorCode(200, "成功");

    // ========== 客户端错误段 ==========

    ErrorCode BAD_REQUEST = new ErrorCode(400, "请求参数不正确");
    ErrorCode UNAUTHORIZED = new ErrorCode(401, "账号未登录");
    ErrorCode FORBIDDEN = new ErrorCode(403, "没有该操作权限");
    ErrorCode NOT_FOUND = new ErrorCode(404, "请求未找到");
    ErrorCode METHOD_NOT_ALLOWED = new ErrorCode(405, "请求方法不正确");

    /**
     * 并发请求，不允许
     */
    ErrorCode LOCKED = new ErrorCode(423, "请求失败，请稍后重试");
    ErrorCode TOO_MANY_REQUESTS = new ErrorCode(429, "请求过于频繁，请稍后重试");

    // ========== 服务端错误段 ==========

    ErrorCode INTERNAL_SERVER_ERROR = new ErrorCode(500, "系统异常, 请联系管理员");
    ErrorCode SERVICE_UNAVAILABLE = new ErrorCode(503, "服务不可用");
    ErrorCode GATEWAY_TIMEOUT = new ErrorCode(504, "网关超时");

    // ========== 自定义错误段 ==========

    /**
     * 重复请求
     */
    ErrorCode REPEATED_REQUESTS = new ErrorCode(900, "重复请求，请稍后重试");
    ErrorCode DEMO_DENY = new ErrorCode(901, "演示模式，禁止写操作");

    ErrorCode UNKNOWN = new ErrorCode(999, "未知错误");

    /**
     * 业务异常
     */
    ErrorCode BUSINESS_EXCEPTION = new ErrorCode(1001, "业务异常，请联系管理员");
    ErrorCode BUILTIN_PARAM_CANNOT_MODIFY = new ErrorCode(1002, "内置参数不能修改，请联系管理员");
}

