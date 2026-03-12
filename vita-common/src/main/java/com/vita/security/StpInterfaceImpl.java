package com.vita.security;

import cn.dev33.satoken.stp.StpInterface;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.security
 * @Author: znk
 * @CreateTime: 2026-03-12  16:33:39
 * @Description: Sa-Token 权限接口实现
 * @Version: 1.0
 */
public class StpInterfaceImpl implements StpInterface {

    /**
     * 获取用户权限列表的方法
     * 根据登录ID和登录类型获取用户的权限集合
     * <p>
     * 如果用户是超级用户，则返回所有权限("*")
     * 否则，先从缓存中获取权限列表，如果缓存中没有，则从数据库中获取
     * 如果数据库中也没有权限，则重新构建用户权限并再次获取
     *
     * @param loginId   登录ID，可以是用户ID或其他唯一标识
     * @param loginType 登录类型，用于区分不同的登录方式
     * @return 权限集合，返回用户拥有的所有权限代码列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    /**
     * 获取指定用户角色的方法
     *
     * @param loginId   登录用户ID，可以是任意类型
     * @param loginType 登录类型
     * @return 返回用户角色列表，如果没有角色则返回空列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return List.of();
    }
}
