package com.vita.aichat.gateway;

import com.vita.core.exception.ServiceException;

/**
 * AI 模型网关异常，保留内部错误码与对外业务异常。
 */
public class AiModelGatewayException extends RuntimeException {

    private final String modelErrorCode;

    private final ServiceException serviceException;

    public AiModelGatewayException(String modelErrorCode, String message, ServiceException serviceException) {
        super(message);
        this.modelErrorCode = modelErrorCode;
        this.serviceException = serviceException;
    }

    public String getModelErrorCode() {
        return modelErrorCode;
    }

    public ServiceException toServiceException() {
        return serviceException;
    }
}
