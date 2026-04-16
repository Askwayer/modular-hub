package com.vita.aichat.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送消息请求。
 */
@Data
public class AiMessageSendDto {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 20000, message = "消息内容长度不能超过20000")
    private String content;

    @Size(max = 16, message = "消息类型长度不能超过16")
    private String messageType = "TEXT";

    private Byte useContext = 1;

    @Size(max = 64, message = "requestId 长度不能超过64")
    private String requestId;
}
