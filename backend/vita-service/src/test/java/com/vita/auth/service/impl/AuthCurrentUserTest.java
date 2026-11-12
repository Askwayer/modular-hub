package com.vita.auth.service.impl;

import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.auth.model.LoginUserInfoModel;
import com.vita.auth.service.IAuthCommonService;
import com.vita.auth.service.IAuthTokenService;
import com.vita.core.exception.ServiceException;
import com.vita.log.model.RequestClientInfo;
import com.vita.log.resolver.RequestClientInfoResolver;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuthCurrentUserTest {

    private final ISysUserService users = mock(ISysUserService.class);
    private final IAuthTokenService tokens = mock(IAuthTokenService.class);
    private final IAuthCommonService authorization = mock(IAuthCommonService.class);
    private final RequestClientInfoResolver resolver = mock(RequestClientInfoResolver.class);
    private final LoginUserInfoContextLoaderImpl loader = new LoginUserInfoContextLoaderImpl(users, tokens, resolver);
    private final AuthServiceImpl service = new AuthServiceImpl(users, null, authorization, null,
            tokens, resolver, null, null, loader);

    @AfterEach
    void cleanup() {
        LoginUserInfoModelContext.removeLoginUserInfo();
    }

    @Test
    void currentInfoShouldReflectRevokedAuthorizationWithoutRelogin() {
        LoginUserInfoModel user = new LoginUserInfoModel();
        user.setId(7L);
        LoginUserInfoModelContext.setLoginUserInfo(user);
        when(authorization.getRoleCodesByUserId(7L)).thenReturn(List.of("BUYER"), List.of());
        when(authorization.getPermissionCodesByUserId(7L)).thenReturn(List.of("purchase:read"), List.of());
        assertThat(service.getCurrentUserInfo().getPermissionCodes()).containsExactly("purchase:read");
        var revokedInfo = service.getCurrentUserInfo();
        assertThat(revokedInfo.getPermissionCodes()).isEmpty();
        assertThat(revokedInfo.getRoleCodes()).isEmpty();
    }

    @Test
    void currentInfoShouldRejectDisabledOrDeletedAccountEvenWithValidLoginId() {
        when(tokens.getLoginId()).thenReturn(7L);
        when(users.getActiveById(7L)).thenReturn(null);
        assertThatThrownBy(service::getCurrentUserInfo).isInstanceOfSatisfying(ServiceException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(401));
        verifyNoInteractions(authorization, resolver);
    }

    @Test
    void loaderShouldReturnFreshAccountFlagsAndRecordedLoginTime() {
        SysUser user = new SysUser();
        user.setId(7L);
        user.setIsSuperAdmin((byte) 0);
        user.setLoginTime(LocalDateTime.of(2026, 11, 12, 10, 0));
        user.setLoginAddress("127.0.0.1");
        when(users.getActiveById(7L)).thenReturn(user);
        when(resolver.resolveCurrentRequest()).thenReturn(new RequestClientInfo());
        LoginUserInfoModel info = loader.loadByUserId(7L);
        assertThat(info.getIsSuperAdmin()).isZero();
        assertThat(info.getLoginTime()).isEqualTo("2026-11-12 10:00:00");
        assertThat(info.getLoginAddress()).isEqualTo("127.0.0.1");
        verify(users, never()).getById(anyLong());
    }

    @Test
    void unknownAccountStatusMustNotAllowLogin() {
        SysUser user = new SysUser();
        user.setStatus((byte) 2);
        assertThatThrownBy(() -> service.checkUserStatus(user)).isInstanceOf(ServiceException.class);
        user.setStatus(null);
        assertThatThrownBy(() -> service.checkUserStatus(user)).isInstanceOf(ServiceException.class);
    }
}
