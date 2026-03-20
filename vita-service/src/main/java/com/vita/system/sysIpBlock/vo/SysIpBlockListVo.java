package com.vita.system.sysIpBlock.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysIpBlock.vo
 * @Author znk
 * @CreateTime 2026-03-21 00:23:39
 * @Description: IP封禁表 列表 VO
 * @version 1.0
 */
@Data
public class SysIpBlockListVo {
    /**
     * IP地址
     */
    private String ip;

    /**
     * 状态：0-生效 1-失效
     */
    private Boolean status;

    /**
     * 备注
     */
    private String remark;

}
