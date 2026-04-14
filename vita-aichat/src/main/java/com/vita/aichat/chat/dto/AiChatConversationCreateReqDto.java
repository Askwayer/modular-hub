package com.vita.aichat.chat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建会话请求。
 */
@Data
public class AiChatConversationCreateReqDto {

    @Size(max = 200, message = "会话标题长度不能超过 200")
    private String title;

    @Size(max = 4000, message = "系统提示词长度不能超过 4000")
    private String systemPrompt;

    @Size(max = 64, message = "模型编码长度不能超过 64")
    private String modelCode;

    @Min(value = 0, message = "温度参数不能小于 0")
    @Max(value = 2, message = "温度参数不能大于 2")
    private BigDecimal temperature;

    @Min(value = 1, message = "最大输出 token 不能小于 1")
    @Max(value = 65535, message = "最大输出 token 不能超过 65535")
    private Integer maxTokens;

    @Min(value = 1, message = "最大上下文消息数不能小于 1")
    @Max(value = 100, message = "最大上下文消息数不能超过 100")
    private Integer maxContextMessages;
}
