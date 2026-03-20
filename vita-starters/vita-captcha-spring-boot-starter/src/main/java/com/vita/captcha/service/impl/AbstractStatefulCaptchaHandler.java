package com.vita.captcha.service.impl;

import com.vita.captcha.dto.CaptchaVerifyRequest;
import com.vita.captcha.enums.CaptchaType;
import com.vita.captcha.exception.CaptchaErrorCode;
import com.vita.captcha.model.CaptchaChallenge;
import com.vita.captcha.model.CaptchaState;
import com.vita.captcha.model.CaptchaVerifyResult;
import com.vita.captcha.properties.CaptchaProperties;
import com.vita.captcha.service.CaptchaHandler;
import com.vita.captcha.service.CaptchaStore;
import com.vita.core.exception.ServiceException;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.service.impl
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 通用状态型验证码处理器
 */
public abstract class AbstractStatefulCaptchaHandler implements CaptchaHandler {

    protected final CaptchaProperties captchaProperties;

    protected final CaptchaStore store;

    private final CaptchaType type;

    protected AbstractStatefulCaptchaHandler(CaptchaType type, CaptchaProperties captchaProperties, CaptchaStore store) {
        this.type = type;
        this.captchaProperties = captchaProperties;
        this.store = store;
    }

    /**
     * 获取当前处理器支持的验证码类型。
     *
     * @return 验证码类型
     */
    @Override
    public CaptchaType getType() {
        return type;
    }

    /**
     * 生成验证码挑战。
     *
     * @return 验证码挑战结果
     */
    @Override
    public CaptchaChallenge generate() {
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireAt = LocalDateTime.now().plus(captchaProperties.getTtl());
        CaptchaState state = buildState(captchaId, expireAt);
        store.save(state);
        return new CaptchaChallenge(captchaId, type, expireAt, buildPayload(state));
    }

    /**
     * 校验验证码请求。
     *
     * @param request 验证码校验请求
     * @return 验证码校验结果
     */
    @Override
    public CaptchaVerifyResult verify(CaptchaVerifyRequest request) {
        if (!StringUtils.hasText(request.getCaptchaId())) {
            return CaptchaVerifyResult.failure("验证码不存在或已失效", 0);
        }

        Optional<CaptchaState> optionalState = store.load(request.getCaptchaId());
        if (optionalState.isEmpty()) {
            return CaptchaVerifyResult.failure("验证码不存在或已失效", 0);
        }

        CaptchaState state = optionalState.get();
        if (state.isExpired()) {
            store.delete(state.getCaptchaId());
            return CaptchaVerifyResult.failure("验证码已过期", 0);
        }
        if (state.getType() != type) {
            return CaptchaVerifyResult.failure("验证码类型不匹配", state.remainingAttempts());
        }

        VerificationOutcome outcome = doVerify(state, request);
        if (outcome.success()) {
            store.consume(state.getCaptchaId());
            return CaptchaVerifyResult.success(outcome.message());
        }

        Optional<CaptchaState> updatedState = store.incrementFailCount(state.getCaptchaId());
        int remainingAttempts = updatedState.map(CaptchaState::remainingAttempts).orElse(0);
        if (remainingAttempts == 0) {
            store.delete(state.getCaptchaId());
        }
        return CaptchaVerifyResult.failure(outcome.message(), remainingAttempts);
    }

    /**
     * 创建服务端验证码状态对象。
     *
     * @param captchaId 验证码标识
     * @param expireAt 过期时间
     * @return 验证码状态
     */
    protected CaptchaState createState(String captchaId, LocalDateTime expireAt) {
        CaptchaState state = new CaptchaState();
        state.setCaptchaId(captchaId);
        state.setType(type);
        state.setExpireAt(expireAt);
        state.setMaxFailureCount(captchaProperties.getMaxFailures());
        return state;
    }

    /**
     * 校验文本输入是否为空。
     *
     * @param value 输入值
     * @param message 异常消息
     * @return 去除首尾空格后的文本
     */
    protected String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ServiceException(CaptchaErrorCode.CAPTCHA_REQUEST_INVALID.getCode(), message);
        }
        return value.trim();
    }

    /**
     * 校验滑块偏移量是否为空。
     *
     * @param offset 偏移量
     * @return 偏移量
     */
    protected Integer requireOffset(Integer offset) {
        if (offset == null) {
            throw new ServiceException(CaptchaErrorCode.CAPTCHA_REQUEST_INVALID.getCode(), "滑块偏移量不能为空");
        }
        return offset;
    }

    protected abstract CaptchaState buildState(String captchaId, LocalDateTime expireAt);

    protected abstract Object buildPayload(CaptchaState state);

    protected abstract VerificationOutcome doVerify(CaptchaState state, CaptchaVerifyRequest request);

    protected record VerificationOutcome(boolean success, String message) {

        static VerificationOutcome success(String message) {
            return new VerificationOutcome(true, message);
        }

        static VerificationOutcome failure(String message) {
            return new VerificationOutcome(false, message);
        }
    }
}
