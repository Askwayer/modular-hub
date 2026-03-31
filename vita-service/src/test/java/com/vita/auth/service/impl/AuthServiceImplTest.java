package com.vita.auth.service.impl;

import com.vita.auth.dto.AuthLoginDto;
import com.vita.auth.service.AuthTokenService;
import com.vita.auth.vo.AuthLoginVo;
import com.vita.captcha.service.CaptchaApplicationService;
import com.vita.constant.Constants;
import com.vita.core.exception.ServiceException;
import com.vita.core.model.LoginUserInfoModel;
import com.vita.log.model.RequestClientInfo;
import com.vita.log.resolver.RequestClientInfoResolver;
import com.vita.log.service.LoginAuditService;
import com.vita.system.sysIpBlock.service.ISysIpBlockService;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private ISysUserService sysUserService;
    @Mock
    private ISysIpBlockService sysIpBlockService;
    @Mock
    private ObjectProvider<CaptchaApplicationService> captchaApplicationServiceProvider;
    @Mock
    private CaptchaApplicationService captchaApplicationService;
    @Mock
    private AuthTokenService authTokenService;
    @Mock
    private RequestClientInfoResolver requestClientInfoResolver;
    @Mock
    private LoginAuditService loginAuditService;

    @Test
    void loginShouldBuildSaTokenSessionWhenCaptchaDisabled() {
        RequestClientInfo requestClientInfo = buildRequestClientInfo("127.0.0.1");
        AuthServiceImpl authService = new AuthServiceImpl(
                sysUserService,
                sysIpBlockService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService
        );
        when(captchaApplicationServiceProvider.getIfAvailable()).thenReturn(null);
        when(sysIpBlockService.isBlocked("127.0.0.1")).thenReturn(false);
        when(sysUserService.getByUserName("admin")).thenReturn(buildUser("admin", Constants.ENABLED));
        when(requestClientInfoResolver.resolveCurrentRequest()).thenReturn(requestClientInfo);
        when(authTokenService.getTokenValue()).thenReturn("jwt-token");
        when(authTokenService.getTokenTimeout()).thenReturn(7200L);
        when(authTokenService.getTokenName()).thenReturn("Authorization");
        when(authTokenService.getTokenPrefix()).thenReturn("Bearer");

        AuthLoginDto loginDto = new AuthLoginDto();
        loginDto.setUserName("admin");
        loginDto.setPassword("12345678");
        AuthLoginVo loginVo = authService.login(loginDto);

        assertEquals("Authorization", loginVo.getTokenName());
        assertEquals("jwt-token", loginVo.getTokenValue());
        assertEquals("Bearer", loginVo.getTokenPrefix());
        assertEquals(7200L, loginVo.getExpiresIn());
        assertEquals("admin", loginVo.getUserInfo().getUsername());
        assertEquals("系统管理员", loginVo.getUserInfo().getNickName());
        assertEquals(Constants.ENABLED, loginVo.getUserInfo().getIsSuperAdmin());
        assertEquals(Constants.DISABLED, loginVo.getUserInfo().getIsSystem());
        verify(loginAuditService).record("admin", 1L, requestClientInfo, Constants.SUCCESS, Constants.LOGIN_SUCCESS);
        verify(authTokenService).storeLoginUserInfo(any(LoginUserInfoModel.class));
        verify(authTokenService).login(1L);
    }

    @Test
    void loginShouldAuditAndRejectDisabledUser() {
        RequestClientInfo requestClientInfo = buildRequestClientInfo("127.0.0.1");
        AuthServiceImpl authService = new AuthServiceImpl(
                sysUserService,
                sysIpBlockService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService
        );
        when(captchaApplicationServiceProvider.getIfAvailable()).thenReturn(null);
        when(sysIpBlockService.isBlocked("127.0.0.1")).thenReturn(false);
        when(sysUserService.getByUserName("admin")).thenReturn(buildUser("admin", Constants.DISABLED));
        when(requestClientInfoResolver.resolveCurrentRequest()).thenReturn(requestClientInfo);

        AuthLoginDto loginDto = new AuthLoginDto();
        loginDto.setUserName("admin");
        loginDto.setPassword("12345678");

        ServiceException exception = assertThrows(ServiceException.class, () -> authService.login(loginDto));
        assertEquals("账号已被禁用", exception.getMessage());

        verify(loginAuditService).record("admin", null, requestClientInfo, Constants.FAIL, "账号已被禁用");
    }

    @Test
    void loginShouldAuditWrongPassword() {
        RequestClientInfo requestClientInfo = buildRequestClientInfo("127.0.0.1");
        AuthServiceImpl authService = new AuthServiceImpl(
                sysUserService,
                sysIpBlockService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService
        );
        when(captchaApplicationServiceProvider.getIfAvailable()).thenReturn(null);
        when(sysIpBlockService.isBlocked("127.0.0.1")).thenReturn(false);
        when(sysUserService.getByUserName("admin")).thenReturn(buildUser("admin", Constants.ENABLED));
        when(requestClientInfoResolver.resolveCurrentRequest()).thenReturn(requestClientInfo);

        AuthLoginDto loginDto = new AuthLoginDto();
        loginDto.setUserName("admin");
        loginDto.setPassword("wrong-pass");

        ServiceException exception = assertThrows(ServiceException.class, () -> authService.login(loginDto));
        assertEquals("用户名或密码错误", exception.getMessage());

        verify(loginAuditService).record("admin", null, requestClientInfo, Constants.FAIL, "用户名或密码错误");
    }

    @Test
    void loginShouldVerifyCaptchaWhenEnabled() {
        AuthServiceImpl authService = new AuthServiceImpl(
                sysUserService,
                sysIpBlockService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService
        );
        when(captchaApplicationServiceProvider.getIfAvailable()).thenReturn(captchaApplicationService);
        when(sysIpBlockService.isBlocked("127.0.0.1")).thenReturn(false);
        when(sysUserService.getByUserName("admin")).thenReturn(buildUser("admin", Constants.ENABLED));
        when(requestClientInfoResolver.resolveCurrentRequest()).thenReturn(buildRequestClientInfo("127.0.0.1"));
        when(authTokenService.getTokenValue()).thenReturn("jwt-token");
        when(authTokenService.getTokenTimeout()).thenReturn(7200L);
        when(authTokenService.getTokenName()).thenReturn("Authorization");

        AuthLoginDto loginDto = new AuthLoginDto();
        loginDto.setUserName("admin");
        loginDto.setPassword("12345678");
        loginDto.setCaptchaCode("ABCD");
        loginDto.setCaptchaUuid("uuid-1");
        loginDto.setCaptchaEncryptData("enc-1");
        authService.login(loginDto);

        verify(captchaApplicationService).checkCaptcha(any());
    }

    @Test
    void getCurrentUserInfoShouldReadSaTokenSession() {
        AuthServiceImpl authService = new AuthServiceImpl(
                sysUserService,
                sysIpBlockService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService
        );
        LoginUserInfoModel loginUserInfoModel = new LoginUserInfoModel();
        loginUserInfoModel.setUsername("admin");
        when(authTokenService.getStoredLoginUserInfo()).thenReturn(loginUserInfoModel);

        LoginUserInfoModel currentUserInfo = authService.getCurrentUserInfo();
        assertEquals("admin", currentUserInfo.getUsername());
        verify(authTokenService).checkLogin();
    }

    @Test
    void logoutShouldDelegateToSaToken() {
        AuthServiceImpl authService = new AuthServiceImpl(
                sysUserService,
                sysIpBlockService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService
        );
        authService.logout();
        verify(authTokenService).checkLogin();
        verify(authTokenService).logout();
    }

    private SysUser buildUser(String userName, Byte status) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUserName(userName);
        user.setNickName("系统管理员");
        user.setPassword(com.vita.utils.Sm4Utils.encryptToBase64("12345678"));
        user.setStatus(status);
        user.setIsSuperAdmin(Constants.ENABLED);
        user.setIsSystem(Constants.DISABLED);
        return user;
    }

    private RequestClientInfo buildRequestClientInfo(String ip) {
        RequestClientInfo requestClientInfo = new RequestClientInfo();
        requestClientInfo.setIp(ip);
        requestClientInfo.setLocation("内网IP");
        requestClientInfo.setBrowser("Unknown");
        requestClientInfo.setOs("Unknown");
        requestClientInfo.setRequestTime(LocalDateTime.now());
        return requestClientInfo;
    }
}
