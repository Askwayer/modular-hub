package com.vita.aichat.chat.vo;

import lombok.Data;

/**
 * 发送消息响应。
 */
@Data
public class AiChatSendRespVo {

    private Long conversationId;

    private Long userMessageId;

    private Long assistantMessageId;

    private String requestId;

    private String contentStatus;

    private String modelCode;

    private String content;

    private String reasoningContent;

    private Integer tokenInput;

    private Integer tokenOutput;

    private Integer tokenTotal;
}
