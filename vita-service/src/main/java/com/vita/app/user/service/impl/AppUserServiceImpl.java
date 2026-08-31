package com.vita.app.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.app.user.entity.AppUser;
import com.vita.app.user.mapper.AppUserMapper;
import com.vita.app.user.model.AppUserRegisterCommand;
import com.vita.app.user.service.IAppUserService;
import com.vita.app.user.support.AppCredentialPolicy;
import com.vita.app.user.support.AppPasswordCodec;
import com.vita.core.enums.CommonStatusEnum;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.user.service.impl
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App用户认证领域服务实现
 * @Version: 1.0
 */
@Service
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements IAppUserService {

    private final AppUserMapper appUserMapper;
    private final AppPasswordCodec passwordCodec;

    public AppUserServiceImpl(AppUserMapper appUserMapper, AppPasswordCodec passwordCodec) {
        this.appUserMapper = appUserMapper;
        this.passwordCodec = passwordCodec;
    }

    /**
     * 注册App用户，用户名先规范化再依赖数据库唯一键完成最终并发兜底。
     *
     * @param command 注册命令
     * @return 已创建用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppUser register(AppUserRegisterCommand command) {
        if (command == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        String userName = AppCredentialPolicy.normalizeAndValidateUserName(command.getUserName());
        AppCredentialPolicy.validatePassword(command.getPassword());
        String nickName = AppCredentialPolicy.normalizeNickName(command.getNickName(), userName);
        if (appUserMapper.selectByUserName(userName) != null) {
            throw userNameExistsException();
        }

        AppUser user = new AppUser();
        user.setUserName(userName);
        user.setPassword(passwordCodec.encode(command.getPassword()));
        user.setNickName(nickName);
        user.setStatus(CommonStatusEnum.ENABLED.getCode());
        user.setPwdUpdateDate(LocalDateTime.now());
        try {
            appUserMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw userNameExistsException();
        }
        return user;
    }

    /**
     * 密码登录始终使用统一失败文案，避免向外暴露用户名是否存在。
     *
     * @param userName 用户名
     * @param password 原始密码
     * @return 认证通过的用户
     */
    @Override
    public AppUser authenticate(String userName, String password) {
        String normalizedUserName;
        try {
            normalizedUserName = AppCredentialPolicy.normalizeAndValidateUserName(userName);
            AppCredentialPolicy.validatePassword(password);
        } catch (ServiceException ex) {
            throw loginFailureException();
        }

        AppUser user = appUserMapper.selectByUserName(normalizedUserName);
        if (user == null || !passwordCodec.matches(password, user.getPassword())) {
            throw loginFailureException();
        }
        checkEnabled(user);
        return user;
    }

    /**
     * 查询并校验用户状态，OAuth登录与密码登录共用同一禁用规则。
     *
     * @param userId 用户ID
     * @return 启用用户
     */
    @Override
    public AppUser getRequiredEnabledById(Long userId) {
        if (userId == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        checkEnabled(user);
        return user;
    }

    /**
     * 登录成功后只更新登录审计快照，不修改用户名、密码或账号状态。
     *
     * @param userId       用户ID
     * @param loginAddress 登录地址
     */
    @Override
    public void recordLogin(Long userId, String loginAddress) {
        if (userId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AppUser update = new AppUser();
        update.setId(userId);
        update.setLoginTime(LocalDateTime.now());
        update.setLoginAddress(loginAddress);
        if (appUserMapper.updateById(update) != 1) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
    }

    private void checkEnabled(AppUser user) {
        if (CommonStatusEnum.isDisabled(user.getStatus())) {
            throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "账号已被禁用");
        }
    }

    private ServiceException userNameExistsException() {
        return new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "用户名已存在");
    }

    private ServiceException loginFailureException() {
        return new ServiceException(GlobalErrorCode.UNAUTHORIZED.getCode(), AppCredentialPolicy.LOGIN_FAILURE_MESSAGE);
    }
}
