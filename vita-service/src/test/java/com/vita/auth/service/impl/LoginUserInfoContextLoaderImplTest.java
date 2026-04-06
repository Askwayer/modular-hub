package com.vita.auth.service.impl;

import com.vita.auth.service.IAuthTokenService;
import com.vita.core.model.LoginUserInfoModel;
import com.vita.log.model.RequestClientInfo;
import com.vita.log.resolver.RequestClientInfoResolver;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUserInfoContextLoaderImplTest {

    @Mock
    private ISysUserService sysUserService;
    @Mock
    private IAuthTokenService authTokenService;
    @Mock
    private RequestClientInfoResolver requestClientInfoResolver;

    @Test
    void loadByUserIdShouldBuildLoginSnapshot() {
        LoginUserInfoContextLoaderImpl loader = new LoginUserInfoContextLoaderImpl(
                sysUserService,
                authTokenService,
                requestClientInfoResolver
        );
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUserName("admin");
        user.setNickName("系统管理员");
        user.setDeptId(10L);
        user.setAvatarUrl("avatar.png");
        user.setIsSuperAdmin((byte) 1);
        user.setIsSystem((byte) 0);

        RequestClientInfo requestClientInfo = new RequestClientInfo();
        requestClientInfo.setIp("127.0.0.1");
        requestClientInfo.setLocation("内网IP");
        requestClientInfo.setBrowser("Chrome");
        requestClientInfo.setOs("macOS");
        requestClientInfo.setRequestTime(LocalDateTime.of(2026, 4, 6, 18, 0, 0));

        when(sysUserService.getById(1L)).thenReturn(user);
        when(authTokenService.getTokenValue()).thenReturn("jwt-token");
        when(authTokenService.getTokenName()).thenReturn("Authorization");
        when(authTokenService.getTokenPrefix()).thenReturn("Bearer");
        when(requestClientInfoResolver.resolveCurrentRequest()).thenReturn(requestClientInfo);

        LoginUserInfoModel loginUserInfoModel = loader.loadByUserId(1L);

        assertEquals(1L, loginUserInfoModel.getId());
        assertEquals("admin", loginUserInfoModel.getUsername());
        assertEquals("jwt-token", loginUserInfoModel.getToken());
        assertEquals("Authorization", loginUserInfoModel.getTokenName());
        assertEquals("Bearer", loginUserInfoModel.getTokenPrefix());
        assertEquals("127.0.0.1", loginUserInfoModel.getLoginAddress());
        assertEquals("Chrome", loginUserInfoModel.getBrowser());
        assertEquals("2026-04-06 18:00:00", loginUserInfoModel.getLoginTime());
    }

    @Test
    void loadByUserIdShouldReturnNullWhenUserMissing() {
        LoginUserInfoContextLoaderImpl loader = new LoginUserInfoContextLoaderImpl(
                sysUserService,
                authTokenService,
                requestClientInfoResolver
        );
        when(sysUserService.getById(1L)).thenReturn(null);

        assertNull(loader.loadByUserId(1L));
    }
}
