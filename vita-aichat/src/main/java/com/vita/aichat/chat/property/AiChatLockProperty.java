package com.vita.aichat.chat.property;

import lombok.Data;

@Data
public class AiChatLockProperty {

    private AiChatConversationLockProperty conversation = new AiChatConversationLockProperty();
}
