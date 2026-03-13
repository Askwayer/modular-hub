package com.vita.controller.system;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vita.aspect.LogAspect;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.model.LoginUserInfoModel;
import com.vita.interceptor.RequestTraceInterceptor;
import com.vita.log.model.OperLogRecord;
import com.vita.log.service.OperLogPersistence;
import com.vita.nnotation.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogAspectTest {

    @AfterEach
    void tearDown() {
        LoginUserInfoModelContext.removeLoginUserInfo();
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void shouldPersistSuccessAuditAndWriteAccessLog() {
        CapturingOperLogPersistence persistence = new CapturingOperLogPersistence();
        LogAspect aspect = new LogAspect(new StaticObjectProvider<>(persistence));
        TestUserController proxy = createProxy(aspect);

        prepareRequestContext("req-success", "POST", "/system/sysUser/add");
        setCurrentUser(99L, "tester");

        Logger accessLogger = (Logger) LoggerFactory.getLogger("sys-access");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        accessLogger.addAppender(appender);

        Long result = proxy.create(new RequestBody("admin", "secret"));

        assertThat(result).isEqualTo(100L);
        assertThat(persistence.lastRecord).isNotNull();
        assertThat(persistence.lastRecord.getStatus()).isEqualTo((byte) 1);
        assertThat(persistence.lastRecord.getModule()).isEqualTo("系统用户");
        assertThat(persistence.lastRecord.getBusinessType()).isEqualTo("CREATE");
        assertThat(persistence.lastRecord.getOperUserId()).isEqualTo(99L);
        assertThat(persistence.lastRecord.getOperName()).isEqualTo("tester");
        assertThat(persistence.lastRecord.getRequestMethod()).isEqualTo("POST");
        assertThat(persistence.lastRecord.getOperUrl()).endsWith("/system/sysUser/add");
        assertThat(persistence.lastRecord.getRequestParam()).contains("\"password\":\"***\"");
        assertThat(persistence.lastRecord.getResponseResult()).isNull();
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("requestId=req-success");
        assertThat(getTimeThreadLocal()).isNull();

        accessLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void shouldPersistFailureAuditAndRethrowOriginalException() {
        CapturingOperLogPersistence persistence = new CapturingOperLogPersistence();
        LogAspect aspect = new LogAspect(new StaticObjectProvider<>(persistence));
        TestUserController proxy = createProxy(aspect);

        prepareRequestContext("req-failed", "DELETE", "/system/sysUser/delete");
        setCurrentUser(100L, "tester");

        assertThatThrownBy(() -> proxy.delete(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(persistence.lastRecord).isNotNull();
        assertThat(persistence.lastRecord.getStatus()).isEqualTo((byte) 0);
        assertThat(persistence.lastRecord.getErrorMsg()).isEqualTo("IllegalStateException: boom");
        assertThat(persistence.lastRecord.getResponseResult()).isNull();
        assertThat(getTimeThreadLocal()).isNull();
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

    @SuppressWarnings("unchecked")
    private Long getTimeThreadLocal() {
        ThreadLocal<Long> timeThreadLocal = (ThreadLocal<Long>) ReflectionTestUtils.getField(LogAspect.class, "TIME_THREADLOCAL");
        return timeThreadLocal != null ? timeThreadLocal.get() : null;
    }

    private static final class StaticObjectProvider<T> implements ObjectProvider<T> {

        private final T value;

        private StaticObjectProvider(T value) {
            this.value = value;
        }

        @Override
        public T getObject(Object... args) {
            return value;
        }

        @Override
        public T getIfAvailable() {
            return value;
        }

        @Override
        public T getIfUnique() {
            return value;
        }

        @Override
        public T getObject() {
            return value;
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.singletonList(value).iterator();
        }

        @Override
        public Stream<T> stream() {
            return Stream.of(value);
        }

        @Override
        public Stream<T> orderedStream() {
            return Stream.of(value);
        }
    }

    private static final class CapturingOperLogPersistence implements OperLogPersistence {

        private OperLogRecord lastRecord;

        @Override
        public void persist(OperLogRecord record) {
            this.lastRecord = record;
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
