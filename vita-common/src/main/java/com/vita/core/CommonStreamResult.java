package com.vita.core;

import com.vita.core.exception.GlobalErrorCode;
import lombok.Data;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * SSE 场景统一事件结果，和普通 JSON 响应分开建模。
 */
@Data
public class CommonStreamResult<T> implements Serializable {

    private static final long serialVersionUID = -4105139272191683716L;

    /**
     * SSE 事件名称。
     */
    private String event;

    /**
     * 事件是否成功。
     */
    private Boolean success;

    /**
     * 业务状态码。
     */
    private Integer code;

    /**
     * 业务提示信息。
     */
    private String msg;

    /**
     * 请求链路标识。
     */
    private String requestId;

    /**
     * 事件内容。
     */
    private T content;

    public static <T> CommonStreamResult<T> success(String event, String requestId, T content) {
        CommonStreamResult<T> result = new CommonStreamResult<>();
        result.event = event;
        result.success = true;
        result.code = GlobalErrorCode.SUCCESS.getCode();
        result.msg = "接口调用成功";
        result.requestId = requestId;
        result.content = content;
        return result;
    }

    public static <T> CommonStreamResult<T> error(String event,
                                                  String requestId,
                                                  Integer code,
                                                  String message) {
        return error(event, requestId, code, message, null);
    }

    public static <T> CommonStreamResult<T> error(String event,
                                                  String requestId,
                                                  Integer code,
                                                  String message,
                                                  T content) {
        Assert.isTrue(!GlobalErrorCode.SUCCESS.getCode().equals(code), "code 必须是错误的！");
        CommonStreamResult<T> result = new CommonStreamResult<>();
        result.event = event;
        result.success = false;
        result.code = code;
        result.msg = message;
        result.requestId = requestId;
        result.content = content;
        return result;
    }
}