package com.vita.app.oauth.store;

import com.alibaba.fastjson2.JSON;
import com.vita.app.auth.constant.AppAuthConstants;
import com.vita.app.oauth.model.AppOAuthState;
import com.vita.app.oauth.model.AppOAuthTicket;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.redis.RedisCache;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.store
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: Redis OAuth临时状态存储实现
 * @Version: 1.0
 */
@Component
public class RedisAppOAuthFlowStore implements AppOAuthFlowStore {

    private final RedisCache redisCache;

    public RedisAppOAuthFlowStore(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    @Override
    public void saveState(String state, AppOAuthState value, Duration timeout) {
        redisCache.setCacheObject(AppAuthConstants.OAUTH_STATE_KEY_PREFIX + state,
                JSON.toJSONString(value), requirePositiveSeconds(timeout), TimeUnit.SECONDS);
    }

    /**
     * state通过Redis GETDEL原子消费，重复回调即使并发到达也只有一个请求可以继续换取平台令牌。
     *
     * @param state OAuth state
     * @return 一次性流程状态
     */
    @Override
    public AppOAuthState consumeState(String state) {
        String value = redisCache.getAndDeleteCacheObject(AppAuthConstants.OAUTH_STATE_KEY_PREFIX + state);
        if (value == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "OAuth状态无效或已过期");
        }
        return JSON.parseObject(value, AppOAuthState.class);
    }

    @Override
    public void saveTicket(String ticket, AppOAuthTicket value, Duration timeout) {
        redisCache.setCacheObject(AppAuthConstants.OAUTH_TICKET_KEY_PREFIX + ticket,
                JSON.toJSONString(value), requirePositiveSeconds(timeout), TimeUnit.SECONDS);
    }

    /**
     * 登录票据只能兑换一次，避免前端回调URL中的短期票据被重放。
     *
     * @param ticket OAuth票据
     * @return 一次性票据内容
     */
    @Override
    public AppOAuthTicket consumeTicket(String ticket) {
        String value = redisCache.getAndDeleteCacheObject(AppAuthConstants.OAUTH_TICKET_KEY_PREFIX + ticket);
        if (value == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED.getCode(), "OAuth票据无效或已过期");
        }
        return JSON.parseObject(value, AppOAuthTicket.class);
    }

    private long requirePositiveSeconds(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new ServiceException(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), "OAuth临时状态有效期配置错误");
        }
        return Math.max(1L, duration.toSeconds());
    }
}
