package com.vita.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.vita.auth.service.IAuthCommonService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限接口实现。
 *
 * @author znk
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final IAuthCommonService authCommonService;
    public StpInterfaceImpl(IAuthCommonService authCommonService) {
        this.authCommonService = authCommonService;
    }

    /**
     * sa-token 获取当前登录用户权限集合
     *
     * @param loginId   账号id
     * @param loginType 账号类型
     * @return 权限集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 仅为管理账号域加载权限，未知账号域默认拒绝授权。
        if (!StpUtil.TYPE.equals(loginType)) {
            return CollUtil.newArrayList();
        }
        Long userId = resolveUserId(loginId);
        if (userId == null) {
            return CollUtil.newArrayList();
        }
        // 按被查询账号实时取权，不能复用当前请求账号的 Session 授权。
        return authCommonService.getPermissionCodesByUserId(userId);
    }

    /**
     * sa-token 获取当前登录用户角色集合
     *
     * @param loginId   账号id
     * @param loginType 账号类型
     * @return 角色集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 只有默认管理账号域允许回源sys_user角色。
        if (!StpUtil.TYPE.equals(loginType)) {
            return CollUtil.newArrayList();
        }
        Long userId = resolveUserId(loginId);
        if (userId == null) {
            return CollUtil.newArrayList();
        }
        return authCommonService.getRoleCodesByUserId(userId);
    }

    /**
     * 解析登录 ID，尝试转换为 Long 类型。
     *
     * @param loginId 登录 ID
     * @return 解析后的用户 ID，如果无法解析则返回 null
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
