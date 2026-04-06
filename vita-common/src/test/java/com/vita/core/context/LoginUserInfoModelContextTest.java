package com.vita.core.context;

import com.vita.core.exception.ServiceException;
import com.vita.core.model.LoginUserInfoModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginUserInfoModelContextTest {

    @AfterEach
    void tearDown() {
        LoginUserInfoModelContext.removeLoginUserInfo();
    }

    @Test
    void shouldReturnCurrentLoginUserAndUserId() {
        LoginUserInfoModel model = new LoginUserInfoModel();
        model.setId(1L);
        model.setUsername("admin");
        LoginUserInfoModelContext.setLoginUserInfo(model);

        assertTrue(LoginUserInfoModelContext.isLogin());
        assertEquals(model, LoginUserInfoModelContext.getRequiredLoginUserInfo());
        assertEquals(1L, LoginUserInfoModelContext.getRequiredLoginUserId());
        assertEquals("admin", LoginUserInfoModelContext.getRequiredLoginUsername());
    }

    @Test
    void shouldThrowUnauthorizedWhenLoginContextMissing() {
        assertFalse(LoginUserInfoModelContext.isLogin());
        assertThrows(ServiceException.class, LoginUserInfoModelContext::getRequiredLoginUserInfo);
        assertThrows(ServiceException.class, LoginUserInfoModelContext::getRequiredLoginUserId);
        assertThrows(ServiceException.class, LoginUserInfoModelContext::getRequiredLoginUsername);
    }
}
