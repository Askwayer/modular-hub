package com.vita.auth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.auth.constant.AuthConstants;
import com.vita.auth.context.LoginUserInfoContextLoader;
import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.auth.dto.AuthLoginDto;
import com.vita.auth.model.LoginUserInfoModel;
import com.vita.auth.service.IAuthCommonService;
import com.vita.auth.service.IAuthService;
import com.vita.auth.service.IAuthTokenService;
import com.vita.auth.vo.AuthInfoVo;
import com.vita.auth.vo.AuthLoginVo;
import com.vita.captcha.dto.CaptchaVerifyRequestDto;
import com.vita.captcha.service.CaptchaApplicationService;
import com.vita.core.constant.Constants;
import com.vita.core.enums.CommonStatusEnum;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.crypto.Sm4Utils;
import com.vita.log.model.RequestClientInfo;
import com.vita.log.resolver.RequestClientInfoResolver;
import com.vita.log.service.LoginAuditService;
import com.vita.system.sysIpBlock.service.ISysIpBlockService;
import com.vita.system.sysMenu.service.ISysMenuService;
import com.vita.system.sysMenu.vo.RouterVo;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.service.impl
 * @Author: Codex
 * @CreateTime: 2026-03-21
 * @Description: 认证服务实现
 * @Version: 1.0
 */
@Service
public class AuthServiceImpl implements IAuthService {

    private final ISysUserService sysUserService;
    private final ISysIpBlockService sysIpBlockService;
    private final IAuthCommonService authCommonService;
    private final ObjectProvider<CaptchaApplicationService> captchaApplicationServiceProvider;
    private final IAuthTokenService authTokenService;
    private final RequestClientInfoResolver requestClientInfoResolver;
    private final LoginAuditService loginAuditService;
    private final ISysMenuService sysMenuService;
    private final LoginUserInfoContextLoader loginUserInfoContextLoader;

    /**
     * 构造认证服务实现，注入用户查询、验证码、登录态和登录审计等依赖。
     *
     * @param sysUserService                    用户服务
     * @param sysIpBlockService                 IP 封禁服务
     * @param captchaApplicationServiceProvider 验证码应用服务提供者
     * @param authTokenService                  Sa-Token 登录态服务
     * @param requestClientInfoResolver         请求客户端信息解析器
     * @param loginAuditService                 登录审计服务
     */
    public AuthServiceImpl(ISysUserService sysUserService,
                           ISysIpBlockService sysIpBlockService,
                           IAuthCommonService authCommonService,
                           ObjectProvider<CaptchaApplicationService> captchaApplicationServiceProvider,
                           IAuthTokenService authTokenService,
                           RequestClientInfoResolver requestClientInfoResolver,
                           LoginAuditService loginAuditService,
                           ISysMenuService sysMenuService,
                           LoginUserInfoContextLoader loginUserInfoContextLoader) {
        this.sysUserService = sysUserService;
        this.sysIpBlockService = sysIpBlockService;
        this.authCommonService = authCommonService;
        this.captchaApplicationServiceProvider = captchaApplicationServiceProvider;
        this.authTokenService = authTokenService;
        this.requestClientInfoResolver = requestClientInfoResolver;
        this.loginAuditService = loginAuditService;
        this.sysMenuService = sysMenuService;
        this.loginUserInfoContextLoader = loginUserInfoContextLoader;
    }

    /**
     * 执行管理端登录认证，返回令牌与当前登录用户快照。
     *
     * @param loginDto 登录请求参数
     * @return 登录响应
     */
    @Override
    public AuthLoginVo login(AuthLoginDto loginDto) {
        RequestClientInfo requestClientInfo = requestClientInfoResolver.resolveCurrentRequest();
        validateLoginRequest(loginDto);
        checkCaptcha(loginDto);
        checkIpBlocked(requestClientInfo.getIp());
        String userName = loginDto.getUserName();
        try {
            SysUser user = loadUser(userName);
            checkUserStatus(user);
            checkPassword(user, loginDto.getPassword());
            return loginSuccess(user, requestClientInfo);
        } catch (ServiceException ex) {
            loginAuditService.record(userName, null, requestClientInfo, Constants.FAIL, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            loginAuditService.record(userName, null, requestClientInfo, Constants.FAIL, "登录异常");
            throw ex;
        }
    }

    /**
     * 注销当前登录用户的认证态。
     */
    @Override
    public void logout() {
        authTokenService.logout();
        LoginUserInfoModelContext.removeLoginUserInfo();
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前登录用户快照
     */
    @Override
    public AuthInfoVo getCurrentUserInfo() {
        resolveCurrentLoginUserInfo();
        LoginUserInfoModel loginUserInfoModel = LoginUserInfoModelContext.getRequiredLoginUserInfo();
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        List<String> roleCodes = authCommonService.getRoleCodesByUserId(userId);
        List<String> permissionCodes = authCommonService.getPermissionCodesByUserId(userId);

        AuthInfoVo authInfoVo = new AuthInfoVo();
        authInfoVo.setUserInfo(loginUserInfoModel);
        authInfoVo.setRoleCodes(roleCodes);
        authInfoVo.setPermissionCodes(permissionCodes);
        return authInfoVo;
    }

    /**
     * 获取路由信息.
     */
    @Override
    public List<RouterVo> getRouters() {
        resolveCurrentLoginUserInfo();
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        return sysMenuService.getRoutersByUserId(userId);
    }

    /**
     * 校验用登录参数的合法性，避免登录流程过早进入数据库查询等操作。
     *
     * @param loginDto 登录请求参数
     */
    private void validateLoginRequest(AuthLoginDto loginDto) {
        if (loginDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        String userName = loginDto.getUserName();
        String password = loginDto.getPassword();
        if (!CharSequenceUtil.isNotBlank(userName) || !CharSequenceUtil.isNotBlank(password)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "用户名和密码不能为空");
        }
        if (userName.length() < AuthConstants.USERNAME_MIN_LENGTH || userName.length() > AuthConstants.USERNAME_MAX_LENGTH) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "用户名长度必须在4到32位之间");
        }
        if (password.length() < AuthConstants.PASSWORD_MIN_LENGTH || password.length() > AuthConstants.PASSWORD_MAX_LENGTH) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "密码长度必须在8到64位之间");
        }
        loginDto.setUserName(userName);
    }

    /**
     * 校验验证码参数，并在验证码插槽启用时完成校验。
     *
     * @param loginDto 登录请求参数
     */
    private void checkCaptcha(AuthLoginDto loginDto) {
        CaptchaApplicationService captchaApplicationService = captchaApplicationServiceProvider.getIfAvailable();
        if (captchaApplicationService == null) {
            return;
        }
        if (!CharSequenceUtil.isNotBlank(loginDto.getCaptchaCode())
                || !CharSequenceUtil.isNotBlank(loginDto.getCaptchaUuid())
                || !CharSequenceUtil.isNotBlank(loginDto.getCaptchaEncryptData())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "验证码参数不能为空");
        }
        CaptchaVerifyRequestDto verifyRequestDto = new CaptchaVerifyRequestDto();
        verifyRequestDto.setCaptchaCode(loginDto.getCaptchaCode());
        verifyRequestDto.setUuid(loginDto.getCaptchaUuid());
        verifyRequestDto.setEncryptData(loginDto.getCaptchaEncryptData());
        verifyRequestDto.setMix(CharSequenceUtil.isNotBlank(loginDto.getCaptchaMix()) ? loginDto.getCaptchaMix() : loginDto.getCaptchaUuid());
        captchaApplicationService.checkCaptcha(verifyRequestDto);
    }

    /**
     * 校验 登录 IP 合法性，例如是否在黑名单中。
     *
     * @param ip ip地址
     */
    private void checkIpBlocked(String ip) {
        if (CharSequenceUtil.isNotBlank(ip) && sysIpBlockService.isBlocked(ip)) {
            throw new ServiceException(GlobalErrorCode.LOCKED.getCode(), "当前IP已被封禁");
        }
    }

    /**
     * 根据用户名加载用户实体，不存在时统一抛出登录失败异常。
     *
     * @param userName 用户名
     * @return 用户实体
     */
    private SysUser loadUser(String userName) {
        SysUser user = sysUserService.getByUserName(userName);
        if (user == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED.getCode(), AuthConstants.LOGIN_FAIL_MESSAGE);
        }
        return user;
    }

    /**
     * 处理登录成功后的会话创建、请求用户上下文与登录审计提交流程。
     *
     * @param user              当前登录用户
     * @param requestClientInfo 当前请求客户端信息
     * @return 登录响应对象
     */
    private AuthLoginVo loginSuccess(SysUser user, RequestClientInfo requestClientInfo) {
        authTokenService.login(user.getId());
        LoginUserInfoModel loginUserInfo = buildLoginUserInfo(user, requestClientInfo);
        LoginUserInfoModelContext.setLoginUserInfo(loginUserInfo);
        loginAuditService.record(user.getUserName(), user.getId(), requestClientInfo, Constants.SUCCESS, Constants.LOGIN_SUCCESS);

        AuthLoginVo authLoginVo = new AuthLoginVo();
        authLoginVo.setTokenName(loginUserInfo.getTokenName());
        authLoginVo.setTokenValue(loginUserInfo.getToken());
        authLoginVo.setTokenPrefix(loginUserInfo.getTokenPrefix());
        authLoginVo.setExpiresIn(authTokenService.getTokenTimeout());
        authLoginVo.setUserId(user.getId());
        authLoginVo.setUsername(user.getUserName());
        authLoginVo.setNickName(user.getNickName());
        return authLoginVo;
    }

    /**
     * 用户状态校验单独收口，避免和主流程耦合。
     *
     * @param user 当前待校验用户
     */
    protected void checkUserStatus(SysUser user) {
        if (user == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED.getCode(), AuthConstants.LOGIN_FAIL_MESSAGE);
        }
        if (Objects.equals(user.getIsDeleted(), Boolean.TRUE)) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED.getCode(), AuthConstants.LOGIN_FAIL_MESSAGE);
        }
        if (!CommonStatusEnum.isEnabled(user.getStatus())) {
            throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "账号已被禁用");
        }
    }

    /**
     * 用户密码校验单独收口，避免和主流程耦合。
     *
     * @param user        当前待校验用户
     * @param rawPassword 前端传入的原始密码
     */
    protected void checkPassword(SysUser user, String rawPassword) {
        String encryptedPassword = Sm4Utils.encryptToBase64(rawPassword);
        if (!Objects.equals(user.getPassword(), encryptedPassword)) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED.getCode(), AuthConstants.LOGIN_FAIL_MESSAGE);
        }
    }

    /**
     * 构建当前登录用户快照，并补齐令牌与终端信息。
     *
     * @param user              当前登录用户
     * @param requestClientInfo 当前请求客户端信息
     * @return 登录用户快照
     */
    private LoginUserInfoModel buildLoginUserInfo(SysUser user, RequestClientInfo requestClientInfo) {
        LoginUserInfoModel loginUserInfo = new LoginUserInfoModel();
        loginUserInfo.setId(user.getId());
        loginUserInfo.setUsername(user.getUserName());
        loginUserInfo.setNickName(user.getNickName());
        loginUserInfo.setDeptId(user.getDeptId());
        loginUserInfo.setToken(authTokenService.getTokenValue());
        loginUserInfo.setTokenName(authTokenService.getTokenName());
        loginUserInfo.setTokenPrefix(authTokenService.getTokenPrefix());
        loginUserInfo.setAvatarUrl(user.getAvatarUrl());
        loginUserInfo.setIsSuperAdmin(user.getIsSuperAdmin());
        loginUserInfo.setIsSystem(user.getIsSystem());
        loginUserInfo.setLoginAddress(requestClientInfo.getIp());
        loginUserInfo.setLoginLocation(requestClientInfo.getLocation());
        loginUserInfo.setBrowser(requestClientInfo.getBrowser());
        loginUserInfo.setOs(requestClientInfo.getOs());
        loginUserInfo.setLoginTime(requestClientInfo.getRequestTime().format(AuthConstants.LOGIN_TIME_FORMATTER));
        return loginUserInfo;
    }


    /**
     * 获取当前登录用户快照的统一入口，只复用当前请求上下文，未命中时实时加载有效账号。
     */
    private void resolveCurrentLoginUserInfo() {
        LoginUserInfoModel loginUserInfoModel = LoginUserInfoModelContext.getLoginUserInfo();
        if (loginUserInfoModel != null) {
            return;
        }

        Long userId = resolveUserId(authTokenService.getLoginId());
        if (userId == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        LoginUserInfoModel rebuiltLoginUserInfo = loginUserInfoContextLoader.loadByUserId(userId);
        if (rebuiltLoginUserInfo == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        LoginUserInfoModelContext.setLoginUserInfo(rebuiltLoginUserInfo);
    }

    /**
     * 将任意登录 ID 安全转换为 Long。
     *
     * @param loginId 登录 ID
     * @return 用户 ID；无法转换时返回 null
     */
    private Long resolveUserId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            long userId = Long.parseLong(String.valueOf(loginId));
            return userId > 0 ? userId : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
