package com.vita.aichat.gateway;

import com.vita.aichat.chat.AiChatConstants;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 使用 Spring AI 对接 Qwen OpenAI-compatible 接口。
 */
@Service
public class SpringAiQwenGatewayService implements AiModelGatewayService {

    private final ChatModel chatModel;

    public SpringAiQwenGatewayService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 发起一次同步模型调用并返回完整回复。
     */
    @Override
    public AiChatGatewayResp chat(AiChatGatewayReq req) {
        try {
            ChatResponse response = chatModel.call(buildPrompt(req));
            return toGatewayResponse(response, req.getModelCode(), null, null);
        } catch (Exception ex) {
            throw mapException(ex);
        }
    }

    /**
     * 发起一次流式模型调用，并把增量输出转换成统一事件。
     */
    @Override
    public void chatStream(AiChatGatewayReq req, AiStreamListener listener) {
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder reasoningBuilder = new StringBuilder();
        final ChatResponse[] lastResponseHolder = new ChatResponse[1];
        listener.onStart(req.getModelCode());
        try {
            chatModel.stream(buildPrompt(req))
                    .timeout(Duration.ofMinutes(5))
                    .doOnNext(response -> {
                        lastResponseHolder[0] = response;
                        String delta = extractText(response);
                        if (StringUtils.hasText(delta)) {
                            contentBuilder.append(delta);
                            listener.onContentDelta(delta);
                        }
                        String reasoningDelta = extractReasoningContent(response);
                        if (StringUtils.hasText(reasoningDelta)) {
                            reasoningBuilder.append(reasoningDelta);
                            listener.onReasoningDelta(reasoningDelta);
                        }
                    })
                    .blockLast();
            listener.onComplete(toGatewayResponse(lastResponseHolder[0], req.getModelCode(),
                    contentBuilder.toString(), reasoningBuilder.toString()));
        } catch (Exception ex) {
            String mappedCode = mapModelErrorCode(ex);
            listener.onError(mappedCode, sanitizeMessage(ex.getMessage()), ex);
        }
    }

    /**
     * 把内部网关请求映射为 Spring AI Prompt。
     */
    Prompt buildPrompt(AiChatGatewayReq req) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(req.getSystemPrompt())) {
            messages.add(new SystemMessage(req.getSystemPrompt()));
        }
        if (!CollectionUtils.isEmpty(req.getMessages())) {
            for (AiChatGatewayMessage item : req.getMessages()) {
                if (!StringUtils.hasText(item.getContent())) {
                    continue;
                }
                messages.add(toMessage(item));
            }
        }
        return new Prompt(messages, buildOptions(req));
    }

    private Message toMessage(AiChatGatewayMessage item) {
        return switch (item.getRole()) {
            case "SYSTEM", "SUMMARY" -> new SystemMessage(item.getContent());
            case "ASSISTANT" -> new AssistantMessage(item.getContent());
            case "USER" -> new UserMessage(item.getContent());
            default -> throw new ServiceException(GlobalErrorCode.BAD_REQUEST)
                    .setMessage("不支持的消息角色: " + item.getRole());
        };
    }

    private OpenAiChatOptions buildOptions(AiChatGatewayReq req) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        if (StringUtils.hasText(req.getModelCode())) {
            builder.model(req.getModelCode());
        }
        if (req.getMaxTokens() != null) {
            builder.maxTokens(req.getMaxTokens());
        }
        if (req.getTemperature() != null) {
            builder.temperature(toDouble(req.getTemperature()));
        }
        return builder.build();
    }

    private Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    private AiChatGatewayResp toGatewayResponse(ChatResponse response, String modelCode, String fallbackContent,
                                                String fallbackReasoningContent) {
        AiChatGatewayResp resp = new AiChatGatewayResp();
        resp.setModelCode(modelCode);
        resp.setContent(StringUtils.hasText(fallbackContent) ? fallbackContent : extractText(response));
        resp.setReasoningContent(StringUtils.hasText(fallbackReasoningContent)
                ? fallbackReasoningContent
                : extractReasoningContent(response));
        ChatResponseMetadata metadata = response != null ? response.getMetadata() : null;
        Usage usage = metadata != null ? metadata.getUsage() : null;
        if (usage != null) {
            resp.setTokenInput(usage.getPromptTokens());
            resp.setTokenOutput(usage.getCompletionTokens());
            resp.setTokenTotal(usage.getTotalTokens());
        }
        return resp;
    }

    private String extractText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        return response.getResult().getOutput().getText();
    }

    @SuppressWarnings("unchecked")
    private String extractReasoningContent(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        Map<String, Object> metadata = response.getResult().getOutput().getMetadata();
        if (CollectionUtils.isEmpty(metadata)) {
            return null;
        }
        Object reasoning = metadata.get("reasoning_content");
        if (reasoning == null) {
            reasoning = metadata.get("reasoningContent");
        }
        if (reasoning instanceof String text && StringUtils.hasText(text)) {
            return text;
        }
        if (reasoning instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof String text) {
            return text;
        }
        if (reasoning instanceof Map<?, ?> map) {
            Object content = ((Map<String, Object>) map).get("content");
            if (content instanceof String text && StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private AiModelGatewayException mapException(Exception ex) {
        String modelErrorCode = mapModelErrorCode(ex);
        ServiceException serviceException = new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE)
                .setMessage(sanitizeMessage(ex.getMessage()))
                .setCode(switch (modelErrorCode) {
                    case AiChatConstants.MODEL_TIMEOUT -> GlobalErrorCode.GATEWAY_TIMEOUT.getCode();
                    case AiChatConstants.MODEL_RATE_LIMIT -> GlobalErrorCode.TOO_MANY_REQUESTS.getCode();
                    case AiChatConstants.MODEL_AUTH_ERROR -> GlobalErrorCode.FORBIDDEN.getCode();
                    default -> GlobalErrorCode.SERVICE_UNAVAILABLE.getCode();
                });
        return new AiModelGatewayException(modelErrorCode, serviceException.getMessage(), serviceException);
    }

    private String mapModelErrorCode(Throwable ex) {
        String message = ex != null && ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (message.contains("401") || message.contains("403") || message.contains("unauthorized")
                || message.contains("forbidden")) {
            return AiChatConstants.MODEL_AUTH_ERROR;
        }
        if (message.contains("429") || message.contains("rate limit")) {
            return AiChatConstants.MODEL_RATE_LIMIT;
        }
        if (message.contains("timeout") || message.contains("504") || message.contains("408")) {
            return AiChatConstants.MODEL_TIMEOUT;
        }
        if (message.contains("connection") || message.contains("network") || message.contains("connect")) {
            return AiChatConstants.MODEL_NETWORK_ERROR;
        }
        if (message.contains("parse") || message.contains("deserialize") || message.contains("invalid")) {
            return AiChatConstants.MODEL_RESPONSE_INVALID;
        }
        return AiChatConstants.MODEL_PROVIDER_ERROR;
    }

    private String sanitizeMessage(String message) {
        return StringUtils.hasText(message) ? message : "模型调用失败";
    }
}
