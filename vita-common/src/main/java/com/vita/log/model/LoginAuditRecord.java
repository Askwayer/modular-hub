package com.vita.log.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录审计快照，用于在认证层与持久化层之间传递登录事件信息。
 *
 * @author znk
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginAuditRecord {

    /**
     * 登录用户ID，登录失败时可能为空。
     */
    private Long userId;

    /**
     * 登录用户名。
     */
    private String userName;

    /**
     * 登录来源 IP。
     */
    private String ipaddr;

    /**
     * 登录归属地。
     */
    private String loginLocation;

    /**
     * 浏览器名称。
     */
    private String browser;

    /**
     * 操作系统名称。
     */
    private String os;

    /**
     * 登录状态。
     */
    private String status;

    /**
     * 审计消息。
     */
    private String msg;

    /**
     * 登录时间。
     */
    private LocalDateTime loginTime;
}
