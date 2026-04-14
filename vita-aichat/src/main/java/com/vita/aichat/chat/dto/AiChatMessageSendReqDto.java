package com.vita.aichat.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送消息请求。
 */
@Data
public class AiChatMessageSendReqDto {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 20000, message = "消息内容长度不能超过 20000")
    private String content;

    @Size(max = 16, message = "消息类型长度不能超过 16")
    private String messageType;

    private Byte useContext;

    @Size(max = 64, message = "请求 ID 长度不能超过 64")
    private String requestId;
}
