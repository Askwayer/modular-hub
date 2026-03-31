package com.vita.log.executor;

import com.vita.log.model.LoginAuditRecord;
import com.vita.log.model.RequestClientInfo;
import com.vita.log.persistence.LoginAuditPersistence;
import com.vita.manager.AsyncManager;
import com.vita.manager.factory.AsyncFactory;
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
     * @param requestId         请求ID
     * @param userName          用户名
     * @param userId            用户ID
     * @param requestClientInfo 请求客户端信息快照
     * @param status            登录状态
     * @param message           审计消息
     */
    public void submit(String requestId,
                       String userName,
                       Long userId,
                       RequestClientInfo requestClientInfo,
                       String status,
                       String message) {
        if (requestClientInfo == null) {
            return;
        }

        //
        LoginAuditPersistence loginAuditPersistence = loginAuditPersistenceProvider.getIfAvailable();
        asyncManager.execute(AsyncFactory.recordLogin(
                requestId,
                buildRecord(userName, userId, requestClientInfo, status, message),
                loginAuditPersistence
        ));
    }

    /**
     * 创建登录审计记录快照，避免异步执行期间请求信息被后续流程修改。
     *
     * @param userName          用户名
     * @param userId            用户ID
     * @param requestClientInfo 请求客户端信息
     * @param status            登录状态
     * @param message           审计消息
     * @return 登录审计快照
     */
    private LoginAuditRecord buildRecord(String userName,
                                         Long userId,
                                         RequestClientInfo requestClientInfo,
                                         String status,
                                         String message) {
        return LoginAuditRecord.builder()
                .userId(userId)
                .userName(userName)
                .ipaddr(requestClientInfo.getIp())
                .loginLocation(requestClientInfo.getLocation())
                .browser(requestClientInfo.getBrowser())
                .os(requestClientInfo.getOs())
                .status(status)
                .msg(message)
                .loginTime(requestClientInfo.getRequestTime())
                .build();
    }
}
