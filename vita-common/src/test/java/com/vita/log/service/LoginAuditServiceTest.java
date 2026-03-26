package com.vita.log.service;

import com.vita.interceptor.RequestTraceInterceptor;
import com.vita.log.executor.LoginAuditAsyncExecutor;
import com.vita.log.model.LoginAuditRecord;
import com.vita.log.model.RequestClientInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoginAuditServiceTest {

    private final CapturingLoginAuditAsyncExecutor loginAuditAsyncExecutor = new CapturingLoginAuditAsyncExecutor();

    private final LoginAuditService loginAuditService = new LoginAuditService(loginAuditAsyncExecutor);

    @AfterEach
    void tearDown() {
        MDC.clear();
        loginAuditAsyncExecutor.reset();
    }

    @Test
    void recordShouldBuildAuditRecordAndDelegateToAsyncExecutor() {
        RequestClientInfo requestClientInfo = buildRequestClientInfo();
        MDC.put(RequestTraceInterceptor.REQUEST_ID_KEY, "request-1");

        loginAuditService.record("admin", 1L, requestClientInfo, "0", "登录成功");

        assertEquals("request-1", loginAuditAsyncExecutor.requestId);
        assertEquals(1L, loginAuditAsyncExecutor.record.getUserId());
        assertEquals("admin", loginAuditAsyncExecutor.record.getUserName());
        assertEquals("127.0.0.1", loginAuditAsyncExecutor.record.getIpaddr());
        assertEquals("内网IP", loginAuditAsyncExecutor.record.getLoginLocation());
        assertEquals("Chrome", loginAuditAsyncExecutor.record.getBrowser());
        assertEquals("Mac OS", loginAuditAsyncExecutor.record.getOs());
        assertEquals("0", loginAuditAsyncExecutor.record.getStatus());
        assertEquals("登录成功", loginAuditAsyncExecutor.record.getMsg());
        assertEquals(requestClientInfo.getRequestTime(), loginAuditAsyncExecutor.record.getLoginTime());
    }

    @Test
    void recordShouldIgnoreWhenRequestClientInfoMissing() {
        loginAuditService.record("admin", 1L, null, "1", "登录失败");

        assertNull(loginAuditAsyncExecutor.requestId);
        assertNull(loginAuditAsyncExecutor.record);
    }

    private RequestClientInfo buildRequestClientInfo() {
        RequestClientInfo requestClientInfo = new RequestClientInfo();
        requestClientInfo.setIp("127.0.0.1");
        requestClientInfo.setLocation("内网IP");
        requestClientInfo.setBrowser("Chrome");
        requestClientInfo.setOs("Mac OS");
        requestClientInfo.setRequestTime(LocalDateTime.now());
        return requestClientInfo;
    }

    private static class CapturingLoginAuditAsyncExecutor extends LoginAuditAsyncExecutor {

        private String requestId;

        private LoginAuditRecord record;

        private CapturingLoginAuditAsyncExecutor() {
            super(null, null);
        }

        @Override
        public void submit(String requestId, LoginAuditRecord record) {
            this.requestId = requestId;
            this.record = record;
        }

        private void reset() {
            this.requestId = null;
            this.record = null;
        }
    }
}
