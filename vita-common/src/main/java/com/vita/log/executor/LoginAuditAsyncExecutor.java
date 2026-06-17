package com.vita.log.executor;

import com.vita.constant.Constants;
import com.vita.log.model.LoginAuditRecord;
import com.vita.log.model.RequestClientInfo;
import com.vita.log.persistence.LoginAuditPersistence;
import com.vita.log.persistence.LoginLocationPersistence;
import com.vita.log.persistence.LoginUserInfoPersistence;
import com.vita.log.service.IpLocationService;
import com.vita.manager.AsyncManager;
import com.vita.manager.factory.AsyncFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 登录审计异步执行器，负责快照收口、持久化扩展点解析与统一任务投递。
 *
 * @author znk
 */
@Component
public class LoginAuditAsyncExecutor {

    private final AsyncManager asyncManager;

    private final ObjectProvider<LoginAuditPersistence> loginAuditPersistenceProvider;

    private final ObjectProvider<LoginLocationPersistence> loginLocationPersistenceProvider;

    private final ObjectProvider<LoginUserInfoPersistence> loginUserInfoPersistenceProvider;

    private final IpLocationService ipLocationService;

    public LoginAuditAsyncExecutor(AsyncManager asyncManager,
                                   ObjectProvider<LoginAuditPersistence> loginAuditPersistenceProvider,
                                   ObjectProvider<LoginLocationPersistence> loginLocationPersistenceProvider,
                                   ObjectProvider<LoginUserInfoPersistence> loginUserInfoPersistenceProvider,
                                   IpLocationService ipLocationService) {
        this.asyncManager = asyncManager;
        this.loginAuditPersistenceProvider = loginAuditPersistenceProvider;
        this.loginLocationPersistenceProvider = loginLocationPersistenceProvider;
        this.loginUserInfoPersistenceProvider = loginUserInfoPersistenceProvider;
        this.ipLocationService = ipLocationService;
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

        LoginAuditPersistence loginAuditPersistence = loginAuditPersistenceProvider.getIfAvailable();
        LoginLocationPersistence loginLocationPersistence = loginLocationPersistenceProvider.getIfAvailable();
        LoginUserInfoPersistence loginUserInfoPersistence = loginUserInfoPersistenceProvider.getIfAvailable();
        LoginAuditRecord loginAuditRecord = buildRecord(userName, userId, requestClientInfo, status, message);
        asyncManager.execute(AsyncFactory.recordLogin(
                requestId,
                loginAuditRecord,
                loginAuditPersistence,
                (record, logId) -> {
                    submitUserLoginInfoUpdate(requestId, record, loginUserInfoPersistence);
                    submitLocationEnrichment(requestId, record, logId, loginLocationPersistence);
                }
        ));
    }

    private void submitUserLoginInfoUpdate(String requestId,
                                           LoginAuditRecord loginAuditRecord,
                                           LoginUserInfoPersistence loginUserInfoPersistence) {
        if (loginUserInfoPersistence == null
                || loginAuditRecord.getUserId() == null
                || !Objects.equals(loginAuditRecord.getStatus(), Constants.SUCCESS)) {
            return;
        }
        asyncManager.execute(AsyncFactory.updateLoginInfo(requestId, loginAuditRecord, loginUserInfoPersistence));
    }

    private void submitLocationEnrichment(String requestId,
                                          LoginAuditRecord loginAuditRecord,
                                          Long logId,
                                          LoginLocationPersistence loginLocationPersistence) {
        if (logId == null
                || loginLocationPersistence == null
                || !ipLocationService.needsAsyncEnrichment(loginAuditRecord.getIpaddr(), loginAuditRecord.getLoginLocation())) {
            return;
        }
        asyncManager.execute(AsyncFactory.enrichLoginLocation(
                requestId,
                logId,
                loginAuditRecord.getIpaddr(),
                ipLocationService,
                loginLocationPersistence
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
