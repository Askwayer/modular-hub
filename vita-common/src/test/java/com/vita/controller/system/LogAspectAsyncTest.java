package com.vita.controller.system;

import com.vita.annotation.Log;
import com.vita.aspect.LogAspect;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.model.LoginUserInfoModel;
import com.vita.interceptor.RequestTraceInterceptor;
import com.vita.log.model.OperLogRecord;
import com.vita.log.service.OperLogAsyncExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogAspectAsyncTest {

    @AfterEach
    void tearDown() {
        LoginUserInfoModelContext.removeLoginUserInfo();
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void shouldSubmitSuccessRecordToAsyncExecutor() {
        RecordingOperLogAsyncExecutor executor = new RecordingOperLogAsyncExecutor();
        LogAspect aspect = new LogAspect(executor);
        TestUserController proxy = createProxy(aspect);

        prepareRequestContext("req-success", "POST", "/system/sysUser/add");
        setCurrentUser(99L, "tester");

        Long result = proxy.create(new RequestBody("admin", "secret"));

        assertThat(result).isEqualTo(100L);
        assertThat(executor.lastRequestId).isEqualTo("req-success");
        assertThat(executor.lastRecord).isNotNull();
        assertThat(executor.lastRecord.getStatus()).isEqualTo((byte) 1);
        assertThat(executor.lastRecord.getOperName()).isEqualTo("tester");
        assertThat(executor.lastRecord.getOperUserId()).isEqualTo(99L);
        assertThat(executor.lastRecord.getRequestParam()).contains("\"password\":\"******\"");
        assertThat(executor.submitCount).isEqualTo(1);
    }

    @Test
    void shouldSubmitFailureRecordAndRethrowOriginalException() {
        RecordingOperLogAsyncExecutor executor = new RecordingOperLogAsyncExecutor();
        LogAspect aspect = new LogAspect(executor);
        TestUserController proxy = createProxy(aspect);

        prepareRequestContext("req-failed", "DELETE", "/system/sysUser/delete");
        setCurrentUser(100L, "tester");

        assertThatThrownBy(() -> proxy.delete(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(executor.lastRequestId).isEqualTo("req-failed");
        assertThat(executor.lastRecord).isNotNull();
        assertThat(executor.lastRecord.getStatus()).isEqualTo((byte) 0);
        assertThat(executor.lastRecord.getErrorMsg()).isEqualTo("IllegalStateException: boom");
        assertThat(executor.submitCount).isEqualTo(1);
    }

    private TestUserController createProxy(LogAspect aspect) {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestUserController());
        proxyFactory.addAspect(aspect);
        proxyFactory.setProxyTargetClass(true);
        return proxyFactory.getProxy();
    }

    private void prepareRequestContext(String requestId, String method, String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
        request.setServerName("localhost");
        request.setScheme("http");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
        MDC.put(RequestTraceInterceptor.REQUEST_ID_KEY, requestId);
    }

    private void setCurrentUser(Long id, String username) {
        LoginUserInfoModel loginUser = new LoginUserInfoModel();
        loginUser.setId(id);
        loginUser.setUsername(username);
        LoginUserInfoModelContext.setLoginUserInfo(loginUser);
    }

    private static class RecordingOperLogAsyncExecutor extends OperLogAsyncExecutor {

        private String lastRequestId;

        private OperLogRecord lastRecord;

        private int submitCount;

        private RecordingOperLogAsyncExecutor() {
            super(null, null);
        }

        @Override
        public void submit(String requestId, OperLogRecord record) {
            this.lastRequestId = requestId;
            this.lastRecord = record;
            this.submitCount++;
        }
    }

    private static final class RequestBody {

        private final String username;

        private final String password;

        private RequestBody(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    static class TestUserController {

        @Log(module = "系统用户", businessType = "CREATE", saveResponseData = false)
        public Long create(RequestBody body) {
            return 100L;
        }

        @Log(module = "系统用户", businessType = "DELETE", saveResponseData = false)
        public Boolean delete(Long id) {
            throw new IllegalStateException("boom");
        }
    }
}
