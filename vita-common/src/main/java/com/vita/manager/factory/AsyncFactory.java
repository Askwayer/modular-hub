package com.vita.manager.factory;

import com.vita.interceptor.RequestTraceInterceptor;
import com.vita.log.model.LoginAuditRecord;
import com.vita.log.model.OperLogRecord;
import com.vita.log.persistence.LoginAuditPersistence;
import com.vita.log.persistence.LoginLocationPersistence;
import com.vita.log.persistence.LoginUserInfoPersistence;
import com.vita.log.persistence.OperLogPersistence;
import com.vita.log.service.IpLocationService;
import com.vita.manager.task.SystemAsyncTask;
import com.vita.manager.task.SystemAsyncTaskExecutorType;
import com.vita.utils.ip.AddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.function.BiConsumer;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.manager.factory
 * @Author: znk
 * @CreateTime: 2026-03-19  15:40:19
 * @Description: 系统异步任务工厂，负责生成系统级异步任务定义
 * @Version: 1.0
 */
public class AsyncFactory {

    private static final String SYSTEM_OPER_LOG_TASK_NAME = "system-oper-log";

    private static final String SYSTEM_LOGIN_LOG_TASK_NAME = "system-login-log";

    private static final String SYSTEM_GEO_ENRICH_TASK_NAME = "system-geo-enrich";

    private static final String SYSTEM_LOGIN_INFO_TASK_NAME = "system-login-info-update";

    private static final Logger ACCESS_LOGGER = LoggerFactory.getLogger("sys-access");

    private static final Logger LOG = LoggerFactory.getLogger(AsyncFactory.class);

    private AsyncFactory() {
    }

    /**
     * 操作日志记录
     *
     * @param requestId          请求ID
     * @param operLog            操作日志信息
     * @param operLogPersistence 操作日志持久化器
     * @return 系统操作日志异步任务定义
     */
    public static SystemAsyncTask recordOper(final String requestId,
                                             final OperLogRecord operLog,
                                             final OperLogPersistence operLogPersistence) {
        return SystemAsyncTask.immediate(
                SYSTEM_OPER_LOG_TASK_NAME,
                SystemAsyncTaskExecutorType.OPER_AUDIT,
                () -> {
                    try {
                        if (requestId != null) {
                            MDC.put(RequestTraceInterceptor.REQUEST_ID_KEY, requestId);
                        }
                        ACCESS_LOGGER.info(
                                "requestId={} module={} businessType={} status={} requestMethod={} operUrl={} operIp={} operUserId={} operName={} costTime={} errorMsg={}",
                                requestId,
                                operLog.getModule(),
                                operLog.getBusinessType(),
                                operLog.getStatus(),
                                operLog.getRequestMethod(),
                                operLog.getOperUrl(),
                                operLog.getOperIp(),
                                operLog.getOperUserId(),
                                operLog.getOperName(),
                                operLog.getCostTime(),
                                operLog.getErrorMsg()
                        );
                        if (operLogPersistence != null) {
                            operLogPersistence.persist(operLog);
                        }
                    } catch (Exception ex) {
                        LOG.error("异步保存系统操作日志失败, requestId={}", requestId, ex);
                    } finally {
                        MDC.remove(RequestTraceInterceptor.REQUEST_ID_KEY);
                    }
                }
        );
    }

    /**
     * 登录审计记录。
     *
     * @param requestId        请求ID
     * @param loginAuditRecord 登录审计快照
     * @param loginAuditPersistence 登录审计持久化器
     * @param afterPersist 登录日志落库后的回调
     * @return 系统登录审计异步任务定义
     */
    public static SystemAsyncTask recordLogin(final String requestId,
                                              final LoginAuditRecord loginAuditRecord,
                                              final LoginAuditPersistence loginAuditPersistence,
                                              final BiConsumer<LoginAuditRecord, Long> afterPersist) {
        return SystemAsyncTask.immediate(
                SYSTEM_LOGIN_LOG_TASK_NAME,
                SystemAsyncTaskExecutorType.LOGIN_AUDIT,
                () -> {
                    try {
                        if (requestId != null) {
                            MDC.put(RequestTraceInterceptor.REQUEST_ID_KEY, requestId);
                        }
                        ACCESS_LOGGER.info(
                                "requestId={} event=login userName={} status={} ip={} location={} browser={} os={} msg={}",
                                requestId,
                                loginAuditRecord.getUserName(),
                                loginAuditRecord.getStatus(),
                                loginAuditRecord.getIpaddr(),
                                loginAuditRecord.getLoginLocation(),
                                loginAuditRecord.getBrowser(),
                                loginAuditRecord.getOs(),
                                loginAuditRecord.getMsg()
                        );
                        Long logId = null;
                        if (loginAuditPersistence != null) {
                            logId = loginAuditPersistence.persist(loginAuditRecord);
                        }
                        if (afterPersist != null) {
                            afterPersist.accept(loginAuditRecord, logId);
                        }
                    } catch (Exception ex) {
                        LOG.error("异步保存登录审计失败, requestId={}", requestId, ex);
                    } finally {
                        MDC.remove(RequestTraceInterceptor.REQUEST_ID_KEY);
                    }
                }
        );
    }

    /**
     * 异步更新用户最近登录信息。
     */
    public static SystemAsyncTask updateLoginInfo(final String requestId,
                                                  final LoginAuditRecord loginAuditRecord,
                                                  final LoginUserInfoPersistence loginUserInfoPersistence) {
        return SystemAsyncTask.immediate(
                SYSTEM_LOGIN_INFO_TASK_NAME,
                SystemAsyncTaskExecutorType.LOGIN_AUDIT,
                () -> {
                    try {
                        if (requestId != null) {
                            MDC.put(RequestTraceInterceptor.REQUEST_ID_KEY, requestId);
                        }
                        if (loginUserInfoPersistence != null) {
                            loginUserInfoPersistence.updateLoginInfo(
                                    loginAuditRecord.getUserId(),
                                    loginAuditRecord.getLoginTime(),
                                    loginAuditRecord.getIpaddr()
                            );
                        }
                    } catch (Exception ex) {
                        LOG.error("异步更新用户登录信息失败, requestId={}, userId={}", requestId, loginAuditRecord.getUserId(), ex);
                    } finally {
                        MDC.remove(RequestTraceInterceptor.REQUEST_ID_KEY);
                    }
                }
        );
    }

    /**
     * 异步补全登录日志地理位置。
     */
    public static SystemAsyncTask enrichLoginLocation(final String requestId,
                                                      final Long logId,
                                                      final String ip,
                                                      final IpLocationService ipLocationService,
                                                      final LoginLocationPersistence loginLocationPersistence) {
        return SystemAsyncTask.immediate(
                SYSTEM_GEO_ENRICH_TASK_NAME,
                SystemAsyncTaskExecutorType.GEO_ENRICH,
                () -> {
                    try {
                        if (requestId != null) {
                            MDC.put(RequestTraceInterceptor.REQUEST_ID_KEY, requestId);
                        }
                        String location = ipLocationService.resolveAndCache(ip);
                        if (logId != null && loginLocationPersistence != null && !AddressUtils.UNKNOWN.equals(location)) {
                            loginLocationPersistence.updateLoginLocation(logId, location);
                        }
                    } catch (Exception ex) {
                        LOG.error("异步补全登录归属地失败, requestId={}, logId={}, ip={}", requestId, logId, ip, ex);
                    } finally {
                        MDC.remove(RequestTraceInterceptor.REQUEST_ID_KEY);
                    }
                }
        );
    }
}
