package com.vita.app.user.model;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.user.model
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App用户注册命令
 * @Version: 1.0
 */
@Data
public class AppUserRegisterCommand {

    private String userName;

    private String password;

    private String nickName;
}
