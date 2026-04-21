package com.vita.aichat.chat.support;

import com.vita.aichat.aiChatRequest.entity.AiChatRequest;
import com.vita.aichat.aiChatRequest.service.IAiChatRequestService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.chat.model.context.ConversationRound;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 基于请求生命周期还原完整问答轮次。
 */
@Service
public class ConversationRoundService {

    private final IAiChatRequestService aiChatRequestService;
    private final IAiMessageService aiMessageService;

    public ConversationRoundService(IAiChatRequestService aiChatRequestService,
                                    IAiMessageService aiMessageService) {
        this.aiChatRequestService = aiChatRequestService;
        this.aiMessageService = aiMessageService;
    }

    public List<ConversationRound> listRecentCompletedRounds(Long conversationId, Long userId, int limit) {
        List<AiChatRequest> requests = aiChatRequestService.listRecentSuccessfulChatRequests(conversationId, userId, limit);
        if (requests.isEmpty()) {
            return List.of();
        }
        Map<Long, AiMessage> messageMap = loadMessageMap(requests);
        List<ConversationRound> rounds = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            AiChatRequest request = requests.get(index);
            AiMessage userMessage = messageMap.get(request.getUserMessageId());
            AiMessage assistantMessage = messageMap.get(request.getAssistantMessageId());
            if (userMessage == null || assistantMessage == null) {
                continue;
            }
            if (!Byte.valueOf((byte) 1).equals(userMessage.getUseContext())
                    || !Byte.valueOf((byte) 1).equals(assistantMessage.getUseContext())) {
                continue;
            }
            if (!"DONE".equals(userMessage.getContentStatus())
                    || !"DONE".equals(assistantMessage.getContentStatus())) {
                continue;
            }
            rounds.add(ConversationRound.builder()
                    .roundNo(index + 1)
                    .chatRequest(request)
                    .userMessage(userMessage)
                    .assistantMessage(assistantMessage)
                    .build());
        }
        rounds.sort(Comparator.comparing((ConversationRound round) -> round.getChatRequest().getCompletedAt(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing((ConversationRound round) -> round.getChatRequest().getId(),
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return rounds;
    }

    private Map<Long, AiMessage> loadMessageMap(List<AiChatRequest> requests) {
        Collection<Long> ids = requests.stream()
                .flatMap(request -> java.util.stream.Stream.of(request.getUserMessageId(), request.getAssistantMessageId()))
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, AiMessage> messageMap = new LinkedHashMap<>();
        for (AiMessage message : aiMessageService.listByIds(ids)) {
            messageMap.put(message.getId(), message);
        }
        return messageMap;
    }
}
