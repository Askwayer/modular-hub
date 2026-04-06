package com.vita.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SystemControllerPermissionAnnotationTest {

    @Test
    void userControllerShouldUseUserPermissionCodes() throws NoSuchMethodException {
        assertPermission(SysUserController.class, "create", "system:user:add");
        assertPermission(SysUserController.class, "update", "system:user:update");
        assertPermission(SysUserController.class, "delete", "system:user:delete");
        assertPermission(SysUserController.class, "list", "system:user:view");
        assertPermission(SysUserController.class, "page", "system:user:view");
    }

    @Test
    void roleControllerShouldUseRolePermissionCodes() throws NoSuchMethodException {
        assertPermission(SysRoleController.class, "create", "system:role:add");
        assertPermission(SysRoleController.class, "update", "system:role:update");
        assertPermission(SysRoleController.class, "delete", "system:role:delete");
        assertPermission(SysRoleController.class, "deleteBatch", "system:role:delete");
        assertPermission(SysRoleController.class, "list", "system:role:view");
        assertPermission(SysRoleController.class, "page", "system:role:view");
        assertPermission(SysRoleController.class, "getByUserId", "system:role:view");
        assertPermission(SysRoleController.class, "getAll", "system:role:view");
        assertPermission(SysRoleController.class, "getOptionListByUserId", "system:role:view");
        assertPermission(SysRoleController.class, "checkRoleNameUnique", "system:role:view");
        assertPermission(SysRoleController.class, "checkRoleCodeUnique", "system:role:view");
        assertPermission(SysRoleController.class, "cancelAuthUser", "system:role:grant");
        assertPermission(SysRoleController.class, "cancelAuthUsers", "system:role:grant");
        assertPermission(SysRoleController.class, "authUsers", "system:role:grant");
        assertPermission(SysRoleController.class, "cancelAuthPermission", "system:role:grant");
        assertPermission(SysRoleController.class, "cancelAuthPermissions", "system:role:grant");
        assertPermission(SysRoleController.class, "authPermissions", "system:role:grant");
    }

    @Test
    void permissionControllerShouldUsePermissionCodes() throws NoSuchMethodException {
        assertPermission(SysPermissionController.class, "create", "system:permission:add");
        assertPermission(SysPermissionController.class, "update", "system:permission:update");
        assertPermission(SysPermissionController.class, "delete", "system:permission:delete");
        assertPermission(SysPermissionController.class, "deleteBatch", "system:permission:delete");
        assertPermission(SysPermissionController.class, "list", "system:permission:view");
        assertPermission(SysPermissionController.class, "page", "system:permission:view");
        assertPermission(SysPermissionController.class, "getAll", "system:permission:view");
        assertPermission(SysPermissionController.class, "getByRoleId", "system:permission:view");
        assertPermission(SysPermissionController.class, "getOptionListByRoleId", "system:permission:view");
        assertPermission(SysPermissionController.class, "checkPermissionCodeUnique", "system:permission:view");
        assertPermission(SysPermissionController.class, "checkAuthTagUnique", "system:permission:view");
    }

    @Test
    void menuDeptAndIpBlockControllersShouldUseResourcePermissionCodes() throws NoSuchMethodException {
        assertPermission(SysMenuController.class, "create", "system:menu:add");
        assertPermission(SysMenuController.class, "update", "system:menu:update");
        assertPermission(SysMenuController.class, "delete", "system:menu:delete");
        assertPermission(SysMenuController.class, "list", "system:menu:view");
        assertPermission(SysMenuController.class, "getMenuTreeList", "system:menu:view");

        assertPermission(SysDeptController.class, "create", "system:dept:add");
        assertPermission(SysDeptController.class, "update", "system:dept:update");
        assertPermission(SysDeptController.class, "delete", "system:dept:delete");
        assertPermission(SysDeptController.class, "list", "system:dept:view");
        assertPermission(SysDeptController.class, "page", "system:dept:view");

        assertPermission(SysIpBlockController.class, "create", "system:ip-block:add");
        assertPermission(SysIpBlockController.class, "update", "system:ip-block:update");
        assertPermission(SysIpBlockController.class, "delete", "system:ip-block:delete");
        assertPermission(SysIpBlockController.class, "list", "system:ip-block:view");
        assertPermission(SysIpBlockController.class, "page", "system:ip-block:view");
    }

    @Test
    void logControllersShouldKeepOnlyReadAndDeleteEndpoints() throws NoSuchMethodException {
        assertFalse(hasMethod(SysOperLogController.class, "create"));
        assertFalse(hasMethod(SysOperLogController.class, "update"));
        assertPermission(SysOperLogController.class, "delete", "system:oper-log:delete");
        assertPermission(SysOperLogController.class, "list", "system:oper-log:view");
        assertPermission(SysOperLogController.class, "page", "system:oper-log:view");

        assertFalse(hasMethod(SysLoginLogController.class, "create"));
        assertFalse(hasMethod(SysLoginLogController.class, "update"));
        assertPermission(SysLoginLogController.class, "delete", "system:login-log:delete");
        assertPermission(SysLoginLogController.class, "list", "system:login-log:view");
        assertPermission(SysLoginLogController.class, "page", "system:login-log:view");
    }

    private void assertPermission(Class<?> controllerClass, String methodName, String permission) throws NoSuchMethodException {
        Method method = Arrays.stream(controllerClass.getMethods())
                .filter(item -> item.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(controllerClass.getSimpleName() + "." + methodName));
        SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
        assertNotNull(annotation, controllerClass.getSimpleName() + "." + methodName + " should declare @SaCheckPermission");
        assertArrayEquals(new String[]{permission}, annotation.value());
    }

    private boolean hasMethod(Class<?> controllerClass, String methodName) {
        return Arrays.stream(controllerClass.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals(methodName));
    }
}
