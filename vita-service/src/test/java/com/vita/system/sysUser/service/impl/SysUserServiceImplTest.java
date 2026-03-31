package com.vita.system.sysUser.service.impl;

import com.vita.constant.Constants;
import com.vita.core.exception.ServiceException;
import com.vita.system.sysUser.dto.SysUserCreateDto;
import com.vita.system.sysUser.dto.SysUserUpdateDto;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    private SysUserServiceImpl sysUserService;

    @BeforeEach
    void setUp() {
        sysUserService = new SysUserServiceImpl();
        ReflectionTestUtils.setField(sysUserService, "sysUserMapper", sysUserMapper);
    }

    @Test
    void createShouldRejectWhenAnotherSuperAdminAlreadyExists() {
        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        SysUserCreateDto createDto = new SysUserCreateDto();
        createDto.setUserName("admin2");
        createDto.setPassword("12345678");
        createDto.setIsSuperAdmin(Constants.ENABLED);

        ServiceException exception = assertThrows(ServiceException.class, () -> sysUserService.create(createDto));

        assertEquals("系统仅允许存在一个超级管理员用户", exception.getMessage());
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void updateShouldRejectWhenPromotingSecondSuperAdmin() {
        SysUser existed = new SysUser();
        existed.setId(2L);
        existed.setIsSuperAdmin(Constants.DISABLED);
        when(sysUserMapper.selectById(2L)).thenReturn(existed);
        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        SysUserUpdateDto updateDto = new SysUserUpdateDto();
        updateDto.setId(2L);
        updateDto.setIsSuperAdmin(Constants.ENABLED);

        ServiceException exception = assertThrows(ServiceException.class, () -> sysUserService.update(updateDto));

        assertEquals("系统仅允许存在一个超级管理员用户", exception.getMessage());
        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }
}
