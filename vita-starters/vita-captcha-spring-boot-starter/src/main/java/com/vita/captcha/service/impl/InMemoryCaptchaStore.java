package com.vita.captcha.service.impl;

import com.vita.captcha.model.CaptchaState;
import com.vita.captcha.properties.CaptchaProperties;
import com.vita.captcha.service.CaptchaStore;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.service.impl
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 内存验证码存储
 */
public class InMemoryCaptchaStore implements CaptchaStore {

    private final ConcurrentMap<String, CaptchaState> states = new ConcurrentHashMap<>();

    private final CaptchaProperties properties;

    public InMemoryCaptchaStore(CaptchaProperties properties) {
        this.properties = properties;
    }

    /**
     * 保存验证码状态。
     *
     * @param state 验证码状态
     */
    @Override
    public void save(CaptchaState state) {
        states.put(buildKey(state.getCaptchaId()), state);
    }

    /**
     * 加载验证码状态。
     *
     * @param captchaId 验证码标识
     * @return 验证码状态
     */
    @Override
    public Optional<CaptchaState> load(String captchaId) {
        String key = buildKey(captchaId);
        CaptchaState state = states.get(key);
        if (state == null) {
            return Optional.empty();
        }
        if (state.isExpired()) {
            states.remove(key);
            return Optional.empty();
        }
        return Optional.of(state);
    }

    /**
     * 消费验证码状态。
     *
     * @param captchaId 验证码标识
     * @return 被消费的验证码状态
     */
    @Override
    public Optional<CaptchaState> consume(String captchaId) {
        String key = buildKey(captchaId);
        CaptchaState state = states.remove(key);
        if (state == null || state.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(state);
    }

    /**
     * 累加验证码失败次数。
     *
     * @param captchaId 验证码标识
     * @return 更新后的验证码状态
     */
    @Override
    public Optional<CaptchaState> incrementFailCount(String captchaId) {
        String key = buildKey(captchaId);
        CaptchaState state = states.computeIfPresent(key, (ignored, current) -> {
            if (current.isExpired()) {
                return null;
            }
            current.setFailureCount(current.getFailureCount() + 1);
            if (current.getFailureCount() >= current.getMaxFailureCount()) {
                return null;
            }
            return current;
        });
        if (state != null) {
            return Optional.of(state);
        }
        return Optional.ofNullable(states.get(key));
    }

    /**
     * 删除验证码状态。
     *
     * @param captchaId 验证码标识
     */
    @Override
    public void delete(String captchaId) {
        states.remove(buildKey(captchaId));
    }

    /**
     * 写入一个已过期状态，便于测试过期流程。
     *
     * @param captchaId 验证码标识
     */
    public void putExpired(String captchaId) {
        CaptchaState state = new CaptchaState();
        state.setCaptchaId(captchaId);
        state.setExpireAt(LocalDateTime.now().minusSeconds(1));
        save(state);
    }

    private String buildKey(String captchaId) {
        return properties.getKeyPrefix() + captchaId;
    }
}
