package com.vita.aichat.chat;

/**
 * AI Chat 常量定义。
 */
public final class AiChatConstants {

    private AiChatConstants() {
    }

    public static final String DEFAULT_TITLE = "新对话";

    public static final String MESSAGE_TYPE_TEXT = "TEXT";

    public static final String EVENT_MESSAGE_START = "message-start";

    public static final String EVENT_MESSAGE_DELTA = "message-delta";

    public static final String EVENT_REASONING_DELTA = "reasoning-delta";

    public static final String EVENT_MESSAGE_END = "message-end";

    public static final String EVENT_ERROR = "error";

    public static final String MODEL_AUTH_ERROR = "MODEL_AUTH_ERROR";

    public static final String MODEL_TIMEOUT = "MODEL_TIMEOUT";

    public static final String MODEL_RATE_LIMIT = "MODEL_RATE_LIMIT";

    public static final String MODEL_PROVIDER_ERROR = "MODEL_PROVIDER_ERROR";

    public static final String MODEL_NETWORK_ERROR = "MODEL_NETWORK_ERROR";

    public static final String MODEL_RESPONSE_INVALID = "MODEL_RESPONSE_INVALID";
}
