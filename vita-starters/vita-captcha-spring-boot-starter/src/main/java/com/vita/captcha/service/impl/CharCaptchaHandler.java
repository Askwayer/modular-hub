package com.vita.captcha.service.impl;

import com.google.code.kaptcha.Producer;
import com.vita.captcha.dto.CaptchaVerifyRequest;
import com.vita.captcha.enums.CaptchaType;
import com.vita.captcha.model.CaptchaState;
import com.vita.captcha.properties.CaptchaProperties;
import com.vita.captcha.properties.CharCaptchaProperties;
import com.vita.captcha.service.CaptchaStore;
import com.vita.captcha.util.CaptchaImageUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.service.impl
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 图形文本验证码处理器
 */
public class CharCaptchaHandler extends AbstractStatefulCaptchaHandler {

    private static final String ANSWER_KEY = "answer";

    private final CharCaptchaProperties properties;

    private final Producer producer;

    public CharCaptchaHandler(CaptchaProperties captchaProperties, CharCaptchaProperties properties,
                              CaptchaStore store, Producer producer) {
        super(CaptchaType.CHAR, captchaProperties, store);
        this.properties = properties;
        this.producer = producer;
    }

    @Override
    protected CaptchaState buildState(String captchaId, LocalDateTime expireAt) {
        CaptchaState state = createState(captchaId, expireAt);
        state.getAttributes().put(ANSWER_KEY, producer.createText());
        return state;
    }

    @Override
    protected Object buildPayload(CaptchaState state) {
        String answer = String.valueOf(state.getAttributes().get(ANSWER_KEY));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("imageBase64", CaptchaImageUtils.toPngDataUri(producer.createImage(answer)));
        payload.put("length", properties.getLength());
        payload.put("caseSensitive", properties.isCaseSensitive());
        payload.put("width", properties.getWidth());
        payload.put("height", properties.getHeight());
        return payload;
    }

    @Override
    protected VerificationOutcome doVerify(CaptchaState state, CaptchaVerifyRequest request) {
        String submitted = requireText(request.getAnswer(), "图形文本验证码不能为空");
        String expected = String.valueOf(state.getAttributes().get(ANSWER_KEY));
        boolean success = properties.isCaseSensitive() ? expected.equals(submitted) : expected.equalsIgnoreCase(submitted);
        return success ? VerificationOutcome.success("验证码校验通过")
                : VerificationOutcome.failure("图形文本验证码错误");
    }
}
