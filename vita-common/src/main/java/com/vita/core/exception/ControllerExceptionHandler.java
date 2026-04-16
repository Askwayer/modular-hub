package com.vita.core.exception;

import com.alibaba.fastjson2.JSON;
import com.vita.core.CommonResult;
import com.vita.core.CommonStreamResult;
import com.vita.interceptor.RequestTraceInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.controller
 * @Author: znk
 * @CreateTime: 2026-03-04  21:18:47
 * @Description: 统一异常处理、数据预处理等
 * @Version: 1.0
 */
@ControllerAdvice
public class ControllerExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ControllerExceptionHandler.class);
    private final ObjectProvider<ExceptionMessageResolver> exceptionMessageResolverProvider;

    public ControllerExceptionHandler(ObjectProvider<ExceptionMessageResolver> exceptionMessageResolverProvider) {
        this.exceptionMessageResolverProvider = exceptionMessageResolverProvider;
    }

    /**
     * 校验异常统一处理
     *
     * @param e
     * @return
     */
    @ExceptionHandler(value = BindException.class)
    @ResponseBody
    public CommonResult validExceptionHandler(BindException e,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        String resolvedMessage = resolveValidationMessage(message);
        LOG.warn("参数校验失败：{}" , resolvedMessage);
        if (isSseRequest(request)) {
            writeSseError(response, GlobalErrorCode.BAD_REQUEST.getCode(), resolvedMessage, request);
            return null;
        }
        return CommonResult.error(resolvedMessage);
    }

    /**
     * 校验异常统一处理
     *
     * @param e 异常
     * @return 返回结果
     */
    @ExceptionHandler(value = ServiceException.class)
    @ResponseBody
    public CommonResult validExceptionHandler(ServiceException e,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        String resolvedMessage = resolveServiceMessage(e);
        LOG.warn("业务异常：{}" , resolvedMessage);
        if (isSseRequest(request)) {
            writeSseError(
                    response,
                    e.getCode() != null ? e.getCode() : GlobalErrorCode.UNKNOWN.getCode(),
                    resolvedMessage,
                    request
            );
            return null;
        }
        return CommonResult.error(e.getCode(), resolvedMessage);
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public CommonResult validExceptionHandler(Exception e,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        LOG.error("系统异常：" , e);
        String resolvedMessage = resolveErrorCodeMessage(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        if (isSseRequest(request)) {
            writeSseError(response, GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), resolvedMessage, request);
            return null;
        }
        return CommonResult.error(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), resolvedMessage);
    }

    private String resolveValidationMessage(String message) {
        ExceptionMessageResolver resolver = exceptionMessageResolverProvider.getIfAvailable();
        return resolver == null ? message : resolver.resolveValidationMessage(message);
    }

    private String resolveServiceMessage(ServiceException exception) {
        ExceptionMessageResolver resolver = exceptionMessageResolverProvider.getIfAvailable();
        return resolver == null
                ? exception.getMessage()
                : resolver.resolveServiceMessage(exception.getCode(), exception.getMessage());
    }

    private String resolveErrorCodeMessage(ErrorCode errorCode) {
        ExceptionMessageResolver resolver = exceptionMessageResolverProvider.getIfAvailable();
        return resolver == null ? errorCode.getMsg() : resolver.resolveErrorCode(errorCode);
    }

    private boolean isSseRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        Object producibleTypes = request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
        if (producibleTypes instanceof Set<?> mediaTypes) {
            for (Object mediaType : mediaTypes) {
                if (mediaType instanceof MediaType type
                        && MediaType.TEXT_EVENT_STREAM.isCompatibleWith(type)) {
                    return true;
                }
            }
        }
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    private void writeSseError(HttpServletResponse response,
                               Integer code,
                               String message,
                               HttpServletRequest request) {
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            String payload = JSON.toJSONString(
                    CommonStreamResult.error("error", resolveRequestId(request), code, message)
            );
            response.getWriter().write("event:error\n");
            response.getWriter().write("data:" + payload + "\n\n");
            response.getWriter().flush();
        } catch (IOException ex) {
            LOG.warn("SSE 错误事件发送失败", ex);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestTraceInterceptor.REQUEST_ID_KEY);
        if (requestId != null) {
            return String.valueOf(requestId);
        }
        String headerRequestId = request.getHeader(RequestTraceInterceptor.REQUEST_ID_HEADER);
        return headerRequestId == null ? "" : headerRequestId;
    }
}
