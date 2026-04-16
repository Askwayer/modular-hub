package com.vita.aichat.chat.model.result;

import lombok.Data;

/**
 * 承接一次模型补全后的统一结果。
 */
@Data
public class AiChatCompletionResult {

    private String modelCode;

    private String content;

    private String reasoningContent;

    private Integer tokenInput;

    private Integer tokenOutput;

    private Integer tokenTotal;
}
