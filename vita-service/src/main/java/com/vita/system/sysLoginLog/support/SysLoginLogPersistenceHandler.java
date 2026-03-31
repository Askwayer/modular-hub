package com.vita.system.sysLoginLog.support;

import com.vita.constant.Constants;
import com.vita.log.model.LoginAuditRecord;
import com.vita.log.persistence.LoginAuditPersistence;
import com.vita.system.sysLoginLog.dto.SysLoginLogCreateDto;
import com.vita.system.sysLoginLog.service.ISysLoginLogService;
import com.vita.system.sysUser.service.ISysUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 登录审计持久化适配器，将 common 层登录审计快照转换为系统登录日志实体。
 *
 * @author zhuningkang
 */
@Component
public class SysLoginLogPersistenceHandler implements LoginAuditPersistence {

    @Resource
    private ISysLoginLogService sysLoginLogService;

    @Resource
    private ISysUserService sysUserService;

    /**
     * 持久化登录审计记录，并在登录成功时同步更新用户登录信息。
     *
     * @param record 登录审计记录
     */
    @Override
    public void persist(LoginAuditRecord record) {
        if (record == null) {
            return;
        }

        SysLoginLogCreateDto createDto = new SysLoginLogCreateDto();
        createDto.setUserName(record.getUserName());
        createDto.setIpaddr(record.getIpaddr());
        createDto.setLoginLocation(record.getLoginLocation());
        createDto.setBrowser(record.getBrowser());
        createDto.setOs(record.getOs());
        createDto.setStatus(record.getStatus());
        createDto.setMsg(record.getMsg());
        createDto.setLoginTime(record.getLoginTime());
        sysLoginLogService.create(createDto);

        if (Objects.equals(record.getStatus(), Constants.SUCCESS) && record.getUserId() != null) {
            sysUserService.updateLoginInfo(record.getUserId(), record.getLoginTime(), record.getIpaddr());
        }
    }
}
