package com.vita.aichat.chat.support;

import com.vita.core.CommonResult;
import com.vita.core.CommonStreamResult;
import com.vita.core.exception.ErrorCode;
import com.vita.core.exception.ExceptionMessageResolver;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

/**
 * Aichat WebFlux 异常处理器。
 */
@RestControllerAdvice(basePackages = "com.vita.controller.aichat")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class AiChatReactiveExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AiChatReactiveExceptionHandler.class);

    private final ObjectProvider<ExceptionMessageResolver> exceptionMessageResolverProvider;

    public AiChatReactiveExceptionHandler(ObjectProvider<ExceptionMessageResolver> exceptionMessageResolverProvider) {
        this.exceptionMessageResolverProvider = exceptionMessageResolverProvider;
    }

    @ExceptionHandler({WebExchangeBindException.class, BindException.class})
    public ResponseEntity<?> handleBindException(Exception ex, ServerWebExchange exchange) {
        String message = ex instanceof WebExchangeBindException webExchangeBindException
                ? webExchangeBindException.getAllErrors().get(0).getDefaultMessage()
                : ((BindException) ex).getBindingResult().getAllErrors().get(0).getDefaultMessage();
        String resolvedMessage = resolveValidationMessage(message);
        LOG.warn("参数校验失败：{}", resolvedMessage);
        if (AiChatReactiveHttpSupport.isStreamRequest(exchange)) {
            return ndjsonError(exchange, GlobalErrorCode.BAD_REQUEST.getCode(), resolvedMessage);
        }
        return ResponseEntity.ok(CommonResult.error(resolvedMessage));
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<?> handleServiceException(ServiceException ex, ServerWebExchange exchange) {
        String resolvedMessage = resolveServiceMessage(ex);
        LOG.warn("业务异常：{}", resolvedMessage);
        if (AiChatReactiveHttpSupport.isStreamRequest(exchange)) {
            Integer code = ex.getCode() != null ? ex.getCode() : GlobalErrorCode.UNKNOWN.getCode();
            return ndjsonError(exchange, code, resolvedMessage);
        }
        Integer code = ex.getCode() != null ? ex.getCode() : GlobalErrorCode.UNKNOWN.getCode();
        return ResponseEntity.ok(CommonResult.error(code, resolvedMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex, ServerWebExchange exchange) {
        LOG.error("系统异常：", ex);
        String resolvedMessage = resolveErrorCodeMessage(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        if (AiChatReactiveHttpSupport.isStreamRequest(exchange)) {
            return ndjsonError(exchange, GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), resolvedMessage);
        }
        return ResponseEntity.ok(CommonResult.error(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), resolvedMessage));
    }

    private ResponseEntity<Flux<CommonStreamResult<Object>>> ndjsonError(ServerWebExchange exchange,
                                                                         Integer code,
                                                                         String message) {
        String requestId = AiChatReactiveHttpSupport.resolveRequestId(exchange);
        Flux<CommonStreamResult<Object>> body = Flux.just(
                CommonStreamResult.error("error", requestId, code, message)
        );
        return ResponseEntity.ok()
                .contentType(AiChatReactiveHttpSupport.NDJSON_MEDIA_TYPE)
                .body(body);
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
}
