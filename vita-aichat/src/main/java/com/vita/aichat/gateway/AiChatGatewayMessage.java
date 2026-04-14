package com.vita.aichat.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关消息体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatGatewayMessage {

    private String role;

    private String content;
}
