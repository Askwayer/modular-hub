package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.core.exception.ErrorCode;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * AI Chat 异常转换器。
 *
 * <p>负责将底层异常（网络异常、HTTP 异常等）转换为统一的业务异常。</p>
 */
@Component
public class AiChatExceptionConverter {

    /**
     * 将异常转换为服务异常。
     *
     * @param ex 原始异常
     * @return 服务异常
     */
    public ServiceException convertToServiceException(Exception ex) {
        if (ex instanceof ServiceException serviceException) {
            return serviceException;
        }
        Throwable rootCause = rootCause(ex);
        if (ex instanceof HttpClientErrorException httpClientErrorException) {
            if (httpClientErrorException.getStatusCode().value() == 401
                    || httpClientErrorException.getStatusCode().value() == 403) {
                return new ServiceException(GlobalErrorCode.MODEL_AUTH_ERROR);
            }
            if (httpClientErrorException.getStatusCode().value() == 429) {
                return new ServiceException(GlobalErrorCode.MODEL_RATE_LIMIT);
            }
        }
        if (rootCause instanceof SocketTimeoutException || containsIgnoreCase(ex.getMessage(), "timeout")) {
            return new ServiceException(GlobalErrorCode.MODEL_TIMEOUT);
        }
        if (rootCause instanceof UnknownHostException
                || rootCause instanceof ConnectException
                || rootCause instanceof ResourceAccessException
                || containsIgnoreCase(ex.getMessage(), "connection refused")) {
            return new ServiceException(GlobalErrorCode.MODEL_NETWORK_ERROR);
        }
        if (containsIgnoreCase(ex.getMessage(), "401") || containsIgnoreCase(ex.getMessage(), "403")) {
            return new ServiceException(GlobalErrorCode.MODEL_AUTH_ERROR);
        }
        if (containsIgnoreCase(ex.getMessage(), "429")) {
            return new ServiceException(GlobalErrorCode.MODEL_RATE_LIMIT);
        }
        return new ServiceException(GlobalErrorCode.MODEL_PROVIDER_ERROR.getCode(), fallbackMessage(ex.getMessage(), GlobalErrorCode.MODEL_PROVIDER_ERROR));
    }

    /**
     * 获取异常的根原因。
     *
     * @param throwable 异常
     * @return 根异常
     */
    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * 忽略大小写判断是否包含子串。
     *
     * @param source 源字符串
     * @param target 目标子串
     * @return 是否包含
     */
    private boolean containsIgnoreCase(String source, String target) {
        return CharSequenceUtil.isNotBlank(source) && source.toLowerCase().contains(target.toLowerCase());
    }

    /**
     * 获取回退消息。
     *
     * @param message   原始消息
     * @param errorCode 错误码
     * @return 回退消息
     */
    private String fallbackMessage(String message, ErrorCode errorCode) {
        return CharSequenceUtil.isBlank(message) ? errorCode.getMsg() : message;
    }
}
