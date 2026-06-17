package com.vita.log.executor;

import com.vita.constant.Constants;
import com.vita.log.model.RequestClientInfo;
import com.vita.log.persistence.LoginAuditPersistence;
import com.vita.log.persistence.LoginLocationPersistence;
import com.vita.log.persistence.LoginUserInfoPersistence;
import com.vita.log.service.IpLocationService;
import com.vita.manager.AsyncManager;
import com.vita.manager.task.SystemAsyncTask;
import com.vita.utils.ip.AddressUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAuditAsyncExecutorTest {

    @Mock
    private AsyncManager asyncManager;

    @Mock
    private LoginAuditPersistence loginAuditPersistence;

    @Mock
    private LoginLocationPersistence loginLocationPersistence;

    @Mock
    private LoginUserInfoPersistence loginUserInfoPersistence;

    @Mock
    private IpLocationService ipLocationService;

    private LoginAuditAsyncExecutor loginAuditAsyncExecutor;

    @BeforeEach
    void setUp() {
        loginAuditAsyncExecutor = new LoginAuditAsyncExecutor(
                asyncManager,
                provider(LoginAuditPersistence.class, loginAuditPersistence),
                provider(LoginLocationPersistence.class, loginLocationPersistence),
                provider(LoginUserInfoPersistence.class, loginUserInfoPersistence),
                ipLocationService
        );
        doAnswer(invocation -> {
            SystemAsyncTask task = invocation.getArgument(0);
            task.getTask().run();
            return null;
        }).when(asyncManager).execute(any(SystemAsyncTask.class));
    }

    @Test
    void submitShouldScheduleLoginInfoUpdateAndGeoEnrichment() {
        RequestClientInfo requestClientInfo = new RequestClientInfo();
        requestClientInfo.setIp("8.8.8.8");
        requestClientInfo.setLocation(AddressUtils.UNKNOWN);
        requestClientInfo.setBrowser("Chrome");
        requestClientInfo.setOs("macOS");
        requestClientInfo.setRequestTime(LocalDateTime.of(2026, 6, 17, 10, 0, 0));

        when(loginAuditPersistence.persist(any())).thenReturn(101L);
        when(ipLocationService.needsAsyncEnrichment("8.8.8.8", AddressUtils.UNKNOWN)).thenReturn(true);
        when(ipLocationService.resolveAndCache("8.8.8.8")).thenReturn("California Mountain View");

        loginAuditAsyncExecutor.submit("req-1", "admin", 1L, requestClientInfo, Constants.SUCCESS, "ok");

        verify(loginAuditPersistence).persist(any());
        verify(loginUserInfoPersistence).updateLoginInfo(1L, requestClientInfo.getRequestTime(), "8.8.8.8");
        verify(loginLocationPersistence).updateLoginLocation(101L, "California Mountain View");
        verify(asyncManager, times(3)).execute(any(SystemAsyncTask.class));
    }

    @Test
    void submitShouldOnlyPersistWhenNoFollowUpIsNeeded() {
        RequestClientInfo requestClientInfo = new RequestClientInfo();
        requestClientInfo.setIp("10.0.0.8");
        requestClientInfo.setLocation("内网IP");
        requestClientInfo.setBrowser("Chrome");
        requestClientInfo.setOs("macOS");
        requestClientInfo.setRequestTime(LocalDateTime.of(2026, 6, 17, 11, 0, 0));

        when(loginAuditPersistence.persist(any())).thenReturn(102L);
        when(ipLocationService.needsAsyncEnrichment("10.0.0.8", "内网IP")).thenReturn(false);

        loginAuditAsyncExecutor.submit("req-2", "admin", 1L, requestClientInfo, Constants.FAIL, "bad credentials");

        verify(loginAuditPersistence).persist(any());
        verify(loginUserInfoPersistence, never()).updateLoginInfo(any(), any(), any());
        verify(loginLocationPersistence, never()).updateLoginLocation(any(), any());
        verify(asyncManager, times(1)).execute(any(SystemAsyncTask.class));
        verify(ipLocationService, never()).resolveAndCache(any());
    }

    private <T> ObjectProvider<T> provider(Class<T> type, T bean) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), bean);
        return beanFactory.getBeanProvider(type);
    }
}
