package com.vita.core.exception;

import com.vita.core.CommonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

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

    /**
     * 校验异常统一处理
     *
     * @param e
     * @return
     */
    @ExceptionHandler(value = BindException.class)
    @ResponseBody
    public CommonResult validExceptionHandler(BindException e) {
        LOG.warn("参数校验失败：{}" , e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        return CommonResult.error(e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }

    /**
     * 校验异常统一处理
     *
     * @param e 异常
     * @return 返回结果
     */
    @ExceptionHandler(value = ServiceException.class)
    @ResponseBody
    public CommonResult validExceptionHandler(ServiceException e) {
        LOG.warn("业务异常：{}" , e.getMessage());
        return CommonResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public CommonResult validExceptionHandler(Exception e) {
        LOG.error("系统异常：" , e);
        return CommonResult.error(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }
}
