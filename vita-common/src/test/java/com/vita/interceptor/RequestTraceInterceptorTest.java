package com.vita.interceptor;

import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.model.LoginUserInfoModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestTraceInterceptorTest {

    private final RequestTraceInterceptor interceptor = new RequestTraceInterceptor();

    @AfterEach
    void tearDown() {
        LoginUserInfoModelContext.removeLoginUserInfo();
        MDC.clear();
    }

    @Test
    void preHandleShouldReuseIncomingRequestIdAndClearContextAfterCompletion() throws Exception {
        LoginUserInfoModel staleUser = new LoginUserInfoModel();
        staleUser.setId(1L);
        staleUser.setUsername("stale");
        LoginUserInfoModelContext.setLoginUserInfo(staleUser);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(MDC.get(RequestTraceInterceptor.REQUEST_ID_KEY)).isEqualTo("req-123");
        assertThat(LoginUserInfoModelContext.getLoginUserInfo()).isNull();

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.get(RequestTraceInterceptor.REQUEST_ID_KEY)).isNull();
        assertThat(LoginUserInfoModelContext.getLoginUserInfo()).isNull();
    }

    @Test
    void preHandleShouldGenerateRequestIdWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        boolean proceed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(proceed).isTrue();
        assertThat(MDC.get(RequestTraceInterceptor.REQUEST_ID_KEY)).isNotBlank();
    }
}
