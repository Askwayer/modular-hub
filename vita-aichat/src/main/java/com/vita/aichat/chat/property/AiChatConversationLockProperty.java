package com.vita.aichat.chat.property;

import lombok.Data;

@Data
public class AiChatConversationLockProperty {

    private Long leaseSeconds = 600L;
}
