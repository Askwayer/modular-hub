package com.vita.captcha.controller;

import com.vita.captcha.dto.CaptchaVerifyRequest;
import com.vita.captcha.model.CaptchaChallenge;
import com.vita.captcha.model.CaptchaVerifyResult;
import com.vita.captcha.service.CaptchaFacade;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.controller
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码 控制器
 */
@Validated
@RestController
@ConditionalOnBean(CaptchaFacade.class)
@RequestMapping("/captcha")
public class CaptchaController extends BaseController {

    @Resource
    private CaptchaFacade captchaFacade;

    /**
     * 生成当前启用的验证码挑战。
     *
     * @return 验证码挑战结果
     */
    @PostMapping("/generate")
    public CommonResult<CaptchaChallenge> generate() {
        return CommonResult.success(captchaFacade.generate());
    }

    /**
     * 校验验证码结果。
     *
     * @param request 验证码校验请求
     * @return 验证码校验结果
     */
    @PostMapping("/verify")
    public CommonResult<CaptchaVerifyResult> verify(@RequestBody @Valid CaptchaVerifyRequest request) {
        return CommonResult.success(captchaFacade.verify(request));
    }
}
