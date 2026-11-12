package com.vita.auth;

import cn.dev33.satoken.stp.StpLogic;
import com.vita.auth.config.SaTokenConfigure;
import com.vita.auth.context.LoginUserInfoContextLoader;
import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.auth.interceptor.LoginUserContextInterceptor;
import com.vita.auth.model.LoginUserInfoModel;
import com.vita.auth.property.AuthProperty;
import com.vita.core.exception.ServiceException;
import com.vita.log.interceptor.RequestTraceInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * 使用真实 MVC 拦截器顺序验证有效令牌不能绕过账号状态，且请求间不残留身份。
 */
class LoginUserContextWebTest {

    private StpLogic stpLogic;
    private LoginUserInfoContextLoader loader;
    private ObjectProvider<LoginUserInfoContextLoader> provider;
    private MockMvc mvc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        stpLogic = mock(StpLogic.class);
        loader = mock(LoginUserInfoContextLoader.class);
        provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(loader);
        AuthProperty property = new AuthProperty();
        property.setExtraExcludePaths(List.of("/public"));
        SaTokenConfigure config = new SaTokenConfigure();
        ReflectionTestUtils.setField(config, "requestTraceInterceptor", new RequestTraceInterceptor());
        ReflectionTestUtils.setField(config, "loginUserContextInterceptor",
                new LoginUserContextInterceptor(stpLogic, provider));
        ReflectionTestUtils.setField(config, "authProperty", property);
        ReflectionTestUtils.setField(config, "stpLogic", stpLogic);
        TestRegistry registry = new TestRegistry();
        config.addInterceptors(registry);
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new TestAdvice())
                .addInterceptors(registry.interceptors()).build();
    }

    @AfterEach
    void cleanup() {
        assertThat(LoginUserInfoModelContext.getLoginUserInfo()).isNull();
    }

    @Test
    void disablingLoggedInAccountShouldRejectNextLoginOnlyRequest() throws Exception {
        when(stpLogic.getLoginIdDefaultNull()).thenReturn("7");
        when(loader.loadByUserId(7L)).thenReturn(user(7L, (byte) 1), null);
        mvc.perform(get("/protected")).andExpect(content().string("7:1"));
        mvc.perform(get("/protected")).andExpect(content().string("401"));
        verify(stpLogic, times(2)).checkLogin();
    }

    @Test
    void changedSuperAdminFlagShouldBeVisibleWithoutRelogin() throws Exception {
        when(stpLogic.getLoginIdDefaultNull()).thenReturn(7L);
        when(loader.loadByUserId(7L)).thenReturn(user(7L, (byte) 1), user(7L, (byte) 0));
        mvc.perform(get("/protected")).andExpect(content().string("7:1"));
        mvc.perform(get("/protected")).andExpect(content().string("7:0"));
    }

    @Test
    void mismatchedIdentityOrMissingLoaderShouldDenyAccess() throws Exception {
        when(stpLogic.getLoginIdDefaultNull()).thenReturn(7L);
        when(loader.loadByUserId(7L)).thenReturn(user(8L, (byte) 1));
        mvc.perform(get("/protected")).andExpect(content().string("401"));
        when(provider.getIfAvailable()).thenReturn(null);
        mvc.perform(get("/protected")).andExpect(content().string("401"));
    }

    @Test
    void publicRequestShouldNotInheritPreviousIdentity() throws Exception {
        when(stpLogic.getLoginIdDefaultNull()).thenReturn(7L, null);
        when(loader.loadByUserId(7L)).thenReturn(user(7L, (byte) 0));
        mvc.perform(get("/protected")).andExpect(content().string("7:0"));
        mvc.perform(get("/public")).andExpect(content().string("anonymous"));
    }

    @Test
    void failedAccountLookupMustNotFallBackToEarlierIdentity() throws Exception {
        when(stpLogic.getLoginIdDefaultNull()).thenReturn(7L);
        when(loader.loadByUserId(7L)).thenReturn(user(7L, (byte) 1))
                .thenThrow(new IllegalStateException("account lookup unavailable"));
        mvc.perform(get("/protected")).andExpect(content().string("7:1"));
        assertThatThrownBy(() -> mvc.perform(get("/protected")))
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void invalidLoginIdShouldNotQueryAccounts() throws Exception {
        for (Object id : new Object[]{"bad", 0L, -1L}) {
            when(stpLogic.getLoginIdDefaultNull()).thenReturn(id);
            mvc.perform(get("/protected")).andExpect(content().string("401"));
        }
        verifyNoInteractions(loader);
    }

    private LoginUserInfoModel user(Long id, Byte admin) {
        LoginUserInfoModel info = new LoginUserInfoModel();
        info.setId(id);
        info.setIsSuperAdmin(admin);
        return info;
    }

    private static class TestRegistry extends InterceptorRegistry {
        HandlerInterceptor[] interceptors() {
            return getInterceptors().toArray(HandlerInterceptor[]::new);
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/protected")
        String protectedResource() {
            LoginUserInfoModel info = LoginUserInfoModelContext.getRequiredLoginUserInfo();
            return info.getId() + ":" + info.getIsSuperAdmin();
        }

        @GetMapping("/public")
        String publicResource() {
            return LoginUserInfoModelContext.hasLoginUserInfo() ? "authenticated" : "anonymous";
        }
    }

    @RestControllerAdvice
    static class TestAdvice {
        @ExceptionHandler(ServiceException.class)
        String handle(ServiceException ex) {
            return String.valueOf(ex.getCode());
        }
    }
}
