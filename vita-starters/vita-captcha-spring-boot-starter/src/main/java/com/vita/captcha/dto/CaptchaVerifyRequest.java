package com.vita.captcha.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.dto
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码校验请求
 */
@Data
public class CaptchaVerifyRequest implements Serializable {

    private static final long serialVersionUID = 5911919535466932807L;

    @NotBlank(message = "验证码标识不能为空")
    private String captchaId;

    private String answer;

    private Integer offset;

    private List<Integer> track;
}
