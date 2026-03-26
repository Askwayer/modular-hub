package com.vita.log.executor;

import com.vita.log.model.LoginAuditRecord;
import com.vita.log.persistence.LoginAuditPersistence;
import com.vita.manager.AsyncManager;
import com.vita.manager.factory.AsyncFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 登录审计异步执行器，负责快照收口、持久化扩展点解析与统一任务投递。
 *
 * @author zhuningkang
 */
@Component
public class LoginAuditAsyncExecutor {

    private final AsyncManager asyncManager;

    private final ObjectProvider<LoginAuditPersistence> loginAuditPersistenceProvider;

    public LoginAuditAsyncExecutor(AsyncManager asyncManager,
                                   ObjectProvider<LoginAuditPersistence> loginAuditPersistenceProvider) {
        this.asyncManager = asyncManager;
        this.loginAuditPersistenceProvider = loginAuditPersistenceProvider;
    }

    /**
     * 提交登录审计任务。
     *
     * @param requestId 请求ID
     * @param record    登录审计快照
     */
    public void submit(String requestId, LoginAuditRecord record) {
        if (record == null) {
            return;
        }

        LoginAuditPersistence loginAuditPersistence = loginAuditPersistenceProvider.getIfAvailable();
        asyncManager.execute(AsyncFactory.recordLogin(requestId, snapshot(record), loginAuditPersistence));
    }

    /**
     * 创建登录审计记录快照，避免异步执行期间源对象被后续流程修改。
     *
     * @param source 登录审计源对象
     * @return 登录审计快照
     */
    private LoginAuditRecord snapshot(LoginAuditRecord source) {
        LoginAuditRecord target = new LoginAuditRecord();
        BeanUtils.copyProperties(source, target);
        return target;
    }
}
