package com.vita.aichat.chat.model.memory;

import lombok.Data;

import java.util.List;

/**
 * 结构化记忆载荷。
 */
@Data
public class AiMemoryPayload {

    private String memoryText;

    private String summaryText;

    private List<String> facts;

    private List<String> preferences;

    private List<String> constraints;

    private List<String> openLoops;

    private List<String> pendingTasks;

    private List<String> disallowed;

    private Boolean carryForward;

    private Integer salienceScore;

    private String carryForwardReason;
}
