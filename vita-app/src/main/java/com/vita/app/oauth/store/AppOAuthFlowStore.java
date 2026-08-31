package com.vita.app.oauth.store;

import com.vita.app.oauth.model.AppOAuthState;
import com.vita.app.oauth.model.AppOAuthTicket;

import java.time.Duration;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.store
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: OAuth临时状态存储端口
 * @Version: 1.0
 */
public interface AppOAuthFlowStore {

    void saveState(String state, AppOAuthState value, Duration timeout);

    AppOAuthState consumeState(String state);

    void saveTicket(String ticket, AppOAuthTicket value, Duration timeout);

    AppOAuthTicket consumeTicket(String ticket);
}
