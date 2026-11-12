package com.vita.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: Sa-Token管理账号域RBAC边界测试
 * @Version: 1.0
 */
class StpInterfaceLoginTypeTest {

    @Test
    void unknownLoginTypeShouldNeverLoadAdminRbac() {
        StpInterfaceImpl stpInterface = new StpInterfaceImpl(null);

        assertThat(stpInterface.getRoleList(1L, "unknown")).isEmpty();
        assertThat(stpInterface.getPermissionList(1L, "unknown")).isEmpty();
    }
}
