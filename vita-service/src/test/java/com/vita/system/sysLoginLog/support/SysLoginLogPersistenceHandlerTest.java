package com.vita.system.sysLoginLog.support;

import com.vita.constant.Constants;
import com.vita.log.model.LoginAuditRecord;
import com.vita.system.sysLoginLog.dto.SysLoginLogCreateDto;
import com.vita.system.sysLoginLog.service.ISysLoginLogService;
import com.vita.system.sysUser.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SysLoginLogPersistenceHandlerTest {

    @Mock
    private ISysLoginLogService sysLoginLogService;

    @Mock
    private ISysUserService sysUserService;

    @InjectMocks
    private SysLoginLogPersistenceHandler sysLoginLogPersistenceHandler;

    @Test
    void persistShouldWriteLoginLogAndUpdateUserInfoWhenSuccess() {
        LoginAuditRecord record = buildRecord(Constants.SUCCESS);
        record.setUserId(100L);

        sysLoginLogPersistenceHandler.persist(record);

        ArgumentCaptor<SysLoginLogCreateDto> createCaptor = ArgumentCaptor.forClass(SysLoginLogCreateDto.class);
        verify(sysLoginLogService).create(createCaptor.capture());
        assertEquals("admin", createCaptor.getValue().getUserName());
        assertEquals(Constants.SUCCESS, createCaptor.getValue().getStatus());
        verify(sysUserService).updateLoginInfo(100L, record.getLoginTime(), record.getIpaddr());
    }

    @Test
    void persistShouldOnlyWriteLoginLogWhenFailed() {
        LoginAuditRecord record = buildRecord(Constants.FAIL);
        record.setUserId(100L);

        sysLoginLogPersistenceHandler.persist(record);

        ArgumentCaptor<SysLoginLogCreateDto> createCaptor = ArgumentCaptor.forClass(SysLoginLogCreateDto.class);
        verify(sysLoginLogService).create(createCaptor.capture());
        assertEquals(Constants.FAIL, createCaptor.getValue().getStatus());
        verify(sysUserService, never()).updateLoginInfo(record.getUserId(), record.getLoginTime(), record.getIpaddr());
    }

    private LoginAuditRecord buildRecord(String status) {
        LoginAuditRecord record = new LoginAuditRecord();
        record.setUserName("admin");
        record.setIpaddr("127.0.0.1");
        record.setLoginLocation("内网IP");
        record.setBrowser("Chrome");
        record.setOs("Mac OS");
        record.setStatus(status);
        record.setMsg("测试消息");
        record.setLoginTime(LocalDateTime.now());
        return record;
    }
}
