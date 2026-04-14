package com.vita.aichat.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重命名会话请求。
 */
@Data
public class AiChatConversationRenameReqDto {

    @NotBlank(message = "会话标题不能为空")
    @Size(max = 200, message = "会话标题长度不能超过 200")
    private String title;
}
