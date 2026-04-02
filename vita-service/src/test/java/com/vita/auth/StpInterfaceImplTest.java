package com.vita.auth;

import com.vita.auth.service.IAuthCommonService;
import com.vita.auth.service.IAuthTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StpInterfaceImplTest {

    @Mock
    private IAuthCommonService authCommonService;
    @Mock
    private IAuthTokenService authTokenService;

    @Test
    void shouldReadRoleCodesFromCacheFirst() {
        StpInterfaceImpl stpInterface = new StpInterfaceImpl(authCommonService, authTokenService);
        when(authTokenService.getStoredLoginRoleCodes()).thenReturn(List.of("SUPER_ADMIN"));

        List<String> roleCodes = stpInterface.getRoleList(1L, "login");

        assertEquals(List.of("SUPER_ADMIN"), roleCodes);
        verify(authCommonService, never()).getRoleCodesByUserId(1L);
    }

    @Test
    void shouldLoadPermissionCodesWhenCacheMiss() {
        StpInterfaceImpl stpInterface = new StpInterfaceImpl(authCommonService, authTokenService);
        when(authTokenService.getStoredLoginPermissionCodes()).thenReturn(null);
        when(authCommonService.getPermissionCodesByUserId(1L)).thenReturn(List.of("system:user:view"));

        List<String> permissionCodes = stpInterface.getPermissionList(1L, "login");

        assertEquals(List.of("system:user:view"), permissionCodes);
        verify(authTokenService).storeLoginPermissionCodes(List.of("system:user:view"));
    }
}
