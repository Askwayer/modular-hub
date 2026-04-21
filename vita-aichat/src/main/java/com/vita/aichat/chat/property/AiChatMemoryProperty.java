package com.vita.aichat.chat.property;

import lombok.Data;

/**
 * 内部记忆与滚动压缩配置。
 */
@Data
public class AiChatMemoryProperty {

    /**
     * 是否启用轮次记忆生成。
     */
    private Boolean roundMemoryEnabled = true;

    /**
     * 未滚动轮次记忆达到多少轮后触发压缩。
     */
    private Integer rollupTriggerRounds = 8;

    /**
     * 未滚动轮次记忆达到多少 token 估算后触发压缩。
     */
    private Integer rollupTriggerTokens = 2400;

    /**
     * 最低显著性阈值，低于该值的记忆可被跳过。
     */
    private Integer salienceThreshold = 35;

    /**
     * 历史回填批次大小。
     */
    private Integer backfillBatchSize = 100;
}
