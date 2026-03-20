package com.vita.captcha.service.impl;

import com.vita.captcha.model.CaptchaState;
import com.vita.captcha.properties.CaptchaProperties;
import com.vita.captcha.service.CaptchaStore;
import com.vita.core.RedisCache;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.service.impl
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: Redis 验证码存储
 */
public class RedisCaptchaStore implements CaptchaStore {

    private final RedisCache redisCache;

    private final CaptchaProperties properties;

    public RedisCaptchaStore(RedisCache redisCache, CaptchaProperties properties) {
        this.redisCache = redisCache;
        this.properties = properties;
    }

    /**
     * 保存验证码状态。
     *
     * @param state 验证码状态
     */
    @Override
    public void save(CaptchaState state) {
        long ttlMillis = computeRemainingMillis(state.getExpireAt());
        if (ttlMillis <= 0) {
            return;
        }
        redisCache.setCacheObject(buildKey(state.getCaptchaId()), state, ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 加载验证码状态。
     *
     * @param captchaId 验证码标识
     * @return 验证码状态
     */
    @Override
    public Optional<CaptchaState> load(String captchaId) {
        CaptchaState state = redisCache.getCacheObject(buildKey(captchaId));
        if (state == null) {
            return Optional.empty();
        }
        if (state.isExpired()) {
            delete(captchaId);
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
        Optional<CaptchaState> state = load(captchaId);
        state.ifPresent(ignored -> delete(captchaId));
        return state;
    }

    /**
     * 累加验证码失败次数。
     *
     * @param captchaId 验证码标识
     * @return 更新后的验证码状态
     */
    @Override
    public Optional<CaptchaState> incrementFailCount(String captchaId) {
        Optional<CaptchaState> optionalState = load(captchaId);
        if (optionalState.isEmpty()) {
            return Optional.empty();
        }
        CaptchaState state = optionalState.get();
        state.setFailureCount(state.getFailureCount() + 1);
        if (state.getFailureCount() >= state.getMaxFailureCount()) {
            delete(captchaId);
            return Optional.empty();
        }
        save(state);
        return Optional.of(state);
    }

    /**
     * 删除验证码状态。
     *
     * @param captchaId 验证码标识
     */
    @Override
    public void delete(String captchaId) {
        redisCache.deleteObject(buildKey(captchaId));
    }

    private String buildKey(String captchaId) {
        return properties.getKeyPrefix() + captchaId;
    }

    private long computeRemainingMillis(LocalDateTime expireAt) {
        return Duration.between(LocalDateTime.now(), expireAt).toMillis();
    }
}
