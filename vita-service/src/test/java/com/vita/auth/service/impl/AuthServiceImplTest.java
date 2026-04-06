package com.vita.auth.service.impl;

import com.vita.auth.dto.AuthLoginDto;
import com.vita.auth.service.IAuthCommonService;
import com.vita.auth.service.IAuthTokenService;
import com.vita.auth.vo.AuthInfoVo;
import com.vita.auth.vo.AuthLoginVo;
import com.vita.captcha.service.CaptchaApplicationService;
import com.vita.constant.AuthConstants;
import com.vita.constant.Constants;
import com.vita.core.context.LoginUserInfoContextLoader;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.exception.ServiceException;
import com.vita.core.model.LoginUserInfoModel;
import com.vita.log.model.RequestClientInfo;
import com.vita.log.resolver.RequestClientInfoResolver;
import com.vita.log.service.LoginAuditService;
import com.vita.system.sysIpBlock.service.ISysIpBlockService;
import com.vita.system.sysMenu.service.ISysMenuService;
import com.vita.system.sysMenu.vo.RouterVo;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private ISysUserService sysUserService;
    @Mock
    private ISysIpBlockService sysIpBlockService;
    @Mock
    private IAuthCommonService authCommonService;
    @Mock
    private ObjectProvider<CaptchaApplicationService> captchaApplicationServiceProvider;
    @Mock
    private CaptchaApplicationService captchaApplicationService;
    @Mock
    private IAuthTokenService authTokenService;
    @Mock
    private RequestClientInfoResolver requestClientInfoResolver;
    @Mock
    private LoginAuditService loginAuditService;
    @Mock
    private ISysMenuService sysMenuService;
    @Mock
    private LoginUserInfoContextLoader loginUserInfoContextLoader;

    @AfterEach
    void tearDown() {
        LoginUserInfoModelContext.removeLoginUserInfo();
    }

    @Test
    void loginShouldBuildSaTokenSessionWhenCaptchaDisabled() {
        RequestClientInfo requestClientInfo = buildRequestClientInfo("127.0.0.1");
        AuthServiceImpl authService = new AuthServiceImpl(
                sysUserService,
                sysIpBlockService,
                authCommonService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService,
                sysMenuService,
                loginUserInfoContextLoader
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
        assertEquals(1L, loginVo.getUserId());
        assertEquals("admin", loginVo.getUsername());
        assertEquals("系统管理员", loginVo.getNickName());

        ArgumentCaptor<LoginUserInfoModel> loginUserInfoCaptor = ArgumentCaptor.forClass(LoginUserInfoModel.class);
        verify(loginAuditService).record("admin", 1L, requestClientInfo, Constants.SUCCESS, Constants.LOGIN_SUCCESS);
        verify(authTokenService).storeLoginUserInfo(loginUserInfoCaptor.capture());
        verify(authTokenService).login(1L);
        assertEquals(Constants.ENABLED, loginUserInfoCaptor.getValue().getIsSuperAdmin());
        assertEquals(Constants.DISABLED, loginUserInfoCaptor.getValue().getIsSystem());
    }

    @Test
    void loginShouldAuditAndRejectDisabledUser() {
        RequestClientInfo requestClientInfo = buildRequestClientInfo("127.0.0.1");
        AuthServiceImpl authService = new AuthServiceImpl(
                sysUserService,
                sysIpBlockService,
                authCommonService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService,
                sysMenuService,
                loginUserInfoContextLoader
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
                authCommonService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService,
                sysMenuService,
                loginUserInfoContextLoader
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
                authCommonService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService,
                sysMenuService,
                loginUserInfoContextLoader
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
    void getCurrentUserInfoShouldReadLoginContext() {
        AuthServiceImpl authService = new AuthServiceImpl(
                sysUserService,
                sysIpBlockService,
                authCommonService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService,
                sysMenuService,
                loginUserInfoContextLoader
        );
        LoginUserInfoModel loginUserInfoModel = new LoginUserInfoModel();
        loginUserInfoModel.setId(1L);
        loginUserInfoModel.setUsername("admin");
        LoginUserInfoModelContext.setLoginUserInfo(loginUserInfoModel);
        when(authTokenService.getStoredLoginRoleCodes()).thenReturn(java.util.List.of("SUPER_ADMIN"));
        when(authTokenService.getStoredLoginPermissionCodes()).thenReturn(java.util.List.of(AuthConstants.ALL_PERMISSION_CODE));

        AuthInfoVo currentUserInfo = authService.getCurrentUserInfo();
        assertEquals("admin", currentUserInfo.getUserInfo().getUsername());
        assertEquals("SUPER_ADMIN", currentUserInfo.getRoleCodes().get(0));
        assertEquals(AuthConstants.ALL_PERMISSION_CODE, currentUserInfo.getPermissionCodes().get(0));
        verify(authTokenService).checkLogin();
        verify(authTokenService, never()).getStoredLoginUserInfo();
    }

    @Test
    void getCurrentUserInfoShouldRebuildFromLoaderWhenSessionMissing() {
        AuthServiceImpl authService = new AuthServiceImpl(
                sysUserService,
                sysIpBlockService,
                authCommonService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService,
                sysMenuService,
                loginUserInfoContextLoader
        );
        LoginUserInfoModel loginUserInfoModel = new LoginUserInfoModel();
        loginUserInfoModel.setId(1L);
        loginUserInfoModel.setUsername("admin");
        when(authTokenService.getStoredLoginUserInfo()).thenReturn(null);
        when(authTokenService.getLoginId()).thenReturn(1L);
        when(loginUserInfoContextLoader.loadByUserId(1L)).thenReturn(loginUserInfoModel);
        when(authTokenService.getStoredLoginRoleCodes()).thenReturn(java.util.List.of("SUPER_ADMIN"));
        when(authTokenService.getStoredLoginPermissionCodes()).thenReturn(java.util.List.of(AuthConstants.ALL_PERMISSION_CODE));

        AuthInfoVo currentUserInfo = authService.getCurrentUserInfo();

        assertEquals("admin", currentUserInfo.getUserInfo().getUsername());
        verify(authTokenService).storeLoginUserInfo(loginUserInfoModel);
    }

    @Test
    void getRoutersShouldReadUserIdFromLoginContext() {
        AuthServiceImpl authService = new AuthServiceImpl(
                sysUserService,
                sysIpBlockService,
                authCommonService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService,
                sysMenuService,
                loginUserInfoContextLoader
        );
        LoginUserInfoModel loginUserInfoModel = new LoginUserInfoModel();
        loginUserInfoModel.setId(1L);
        loginUserInfoModel.setUsername("admin");
        LoginUserInfoModelContext.setLoginUserInfo(loginUserInfoModel);

        RouterVo routerVo = new RouterVo();
        routerVo.setRouteName("System");
        routerVo.setRouteLink("/system");
        when(sysMenuService.getRoutersByUserId(1L)).thenReturn(java.util.List.of(routerVo));

        java.util.List<RouterVo> routers = authService.getRouters();

        assertEquals("/system", routers.get(0).getRouteLink());
        verify(authTokenService).checkLogin();
        verify(authTokenService, never()).getLoginId();
    }

    @Test
    void logoutShouldDelegateToSaToken() {
        AuthServiceImpl authService = new AuthServiceImpl(
                sysUserService,
                sysIpBlockService,
                authCommonService,
                captchaApplicationServiceProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService,
                sysMenuService,
                loginUserInfoContextLoader
        );
        LoginUserInfoModel loginUserInfoModel = new LoginUserInfoModel();
        loginUserInfoModel.setId(1L);
        LoginUserInfoModelContext.setLoginUserInfo(loginUserInfoModel);

        authService.logout();

        assertNull(LoginUserInfoModelContext.getLoginUserInfo());
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
