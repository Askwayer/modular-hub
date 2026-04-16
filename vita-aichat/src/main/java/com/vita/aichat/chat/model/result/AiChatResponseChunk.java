package com.vita.aichat.chat.model.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个模型响应片段。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiChatResponseChunk {

    /**
     * 模型提供商请求ID
     */
    private String providerRequestId;

    /**
     * 内容增量
     */
    private String contentDelta;

    /**
     * 推理内容增量
     */
    private String reasoningDelta;

    /**
     * 结束原因
     */
    private String finishReason;

    /**
     * 输入Token数
     */
    private Integer tokenInput;

    /**
     * 输出Token数
     */
    private Integer tokenOutput;

    /**
     * 总Token数
     */
    private Integer tokenTotal;
}
