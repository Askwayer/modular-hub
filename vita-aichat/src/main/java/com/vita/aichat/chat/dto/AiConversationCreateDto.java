package com.vita.aichat.chat.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建会话请求。
 */
@Data
public class AiConversationCreateDto {

    @Size(max = 200, message = "标题长度不能超过200")
    private String title;

    @Size(max = 4000, message = "系统提示词长度不能超过4000")
    private String systemPrompt;

    @Size(max = 64, message = "模型提供者长度不能超过64")
    private String providerCode;

    @Size(max = 64, message = "模型编码长度不能超过64")
    private String modelCode;

    private BigDecimal temperature;

    private Integer maxTokens;

    private Integer maxContextMessages;
}
