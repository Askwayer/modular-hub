package com.vita.captcha.service.impl;

import com.vita.captcha.dto.CaptchaVerifyRequest;
import com.vita.captcha.enums.CaptchaType;
import com.vita.captcha.exception.CaptchaErrorCode;
import com.vita.captcha.model.CaptchaChallenge;
import com.vita.captcha.model.CaptchaState;
import com.vita.captcha.model.CaptchaVerifyResult;
import com.vita.captcha.properties.CaptchaProperties;
import com.vita.captcha.service.CaptchaFacade;
import com.vita.captcha.service.CaptchaHandler;
import com.vita.captcha.service.CaptchaStore;
import com.vita.core.exception.ServiceException;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.service.impl
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 默认验证码门面实现
 */
public class DefaultCaptchaFacade implements CaptchaFacade {

    private final CaptchaProperties properties;

    private final CaptchaStore store;

    private final Map<CaptchaType, CaptchaHandler> handlers = new EnumMap<>(CaptchaType.class);

    public DefaultCaptchaFacade(CaptchaProperties properties, CaptchaStore store, List<CaptchaHandler> captchaHandlers) {
        this.properties = properties;
        this.store = store;
        for (CaptchaHandler handler : captchaHandlers) {
            handlers.put(handler.getType(), handler);
        }
    }

    /**
     * 生成当前启用的验证码挑战。
     *
     * @return 验证码挑战结果
     */
    @Override
    public CaptchaChallenge generate() {
        return getHandler(properties.getCaptchaType()).generate();
    }

    /**
     * 校验验证码请求。
     *
     * @param request 验证码校验请求
     * @return 验证码校验结果
     */
    @Override
    public CaptchaVerifyResult verify(CaptchaVerifyRequest request) {
        if (request == null) {
            throw new ServiceException(CaptchaErrorCode.CAPTCHA_REQUEST_INVALID.getCode(), "验证码请求不能为空");
        }
        return getHandler(resolveType(request)).verify(request);
    }

    private CaptchaType resolveType(CaptchaVerifyRequest request) {
        if (StringUtils.hasText(request.getCaptchaId())) {
            return store.load(request.getCaptchaId())
                    .map(CaptchaState::getType)
                    .orElse(properties.getCaptchaType());
        }
        return properties.getCaptchaType();
    }

    private CaptchaHandler getHandler(CaptchaType type) {
        CaptchaHandler handler = handlers.get(type);
        if (handler == null) {
            throw new ServiceException(CaptchaErrorCode.CAPTCHA_TYPE_UNSUPPORTED);
        }
        return handler;
    }
}
