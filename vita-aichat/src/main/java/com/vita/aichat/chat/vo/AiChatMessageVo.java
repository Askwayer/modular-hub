package com.vita.aichat.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息响应。
 */
@Data
public class AiChatMessageVo {

    private Long id;

    private Long conversationId;

    private String roleType;

    private String messageType;

    private Long replyToId;

    private String requestId;

    private String modelCode;

    private String content;

    private String reasoningContent;

    private String contentStatus;

    private Byte useContext;

    private Integer tokenInput;

    private Integer tokenOutput;

    private Integer tokenTotal;

    private String errorCode;

    private String errorMessage;

    private LocalDateTime createTime;
}
