package com.vita.controller.aichat;

import com.vita.aichat.chat.dto.*;
import com.vita.aichat.chat.service.IAiChatService;
import com.vita.aichat.chat.support.AiChatReactiveBridge;
import com.vita.aichat.chat.support.AiChatReactiveExceptionHandler;
import com.vita.aichat.chat.support.AiChatReactiveHttpSupport;
import com.vita.aichat.chat.vo.*;
import com.vita.core.CommonStreamResult;
import com.vita.core.exception.ExceptionMessageResolver;
import com.vita.core.page.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class AiChatControllerWebFluxTest {

    private final StubAiChatService aiChatService = new StubAiChatService();

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        Executor executor = Runnable::run;
        AiChatController controller = new AiChatController(aiChatService, new AiChatReactiveBridge(executor));
        AiChatReactiveExceptionHandler exceptionHandler = new AiChatReactiveExceptionHandler(emptyExceptionMessageResolverProvider());
        this.webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void shouldReturnNdjsonStreamPayload() {
        AiChatStreamEventVo eventVo = new AiChatStreamEventVo();
        eventVo.setConversationId(1L);
        eventVo.setAssistantMessageId(2L);
        eventVo.setRequestId("req-1");
        eventVo.setContentStatus("GENERATING");
        aiChatService.streamResponse = Flux.just(CommonStreamResult.success("message-start", "req-1", eventVo));

        AiMessageSendDto sendDto = new AiMessageSendDto();
        sendDto.setContent("你好");

        FluxExchangeResult<CommonStreamResult<AiChatStreamEventVo>> result = webTestClient.post()
                .uri("/chat/conversations/1/messages/stream")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .accept(AiChatReactiveHttpSupport.NDJSON_MEDIA_TYPE)
                .bodyValue(sendDto)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(AiChatReactiveHttpSupport.NDJSON_MEDIA_TYPE)
                .returnResult(new ParameterizedTypeReference<>() {
                });

        List<CommonStreamResult<AiChatStreamEventVo>> bodies = result.getResponseBody().collectList().block();
        assertThat(bodies).hasSize(1);
        CommonStreamResult<AiChatStreamEventVo> body = bodies.get(0);
        assertThat(body.getEvent()).isEqualTo("message-start");
        assertThat(body.getRequestId()).isEqualTo("req-1");
        assertThat(body.getContent().getConversationId()).isEqualTo(1L);
    }

    @Test
    void shouldRenderNdjsonErrorWhenStreamRequestValidationFails() {
        FluxExchangeResult<CommonStreamResult<Object>> result = webTestClient.post()
                .uri("/chat/conversations/1/messages/stream")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .accept(AiChatReactiveHttpSupport.NDJSON_MEDIA_TYPE)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(AiChatReactiveHttpSupport.NDJSON_MEDIA_TYPE)
                .returnResult(new ParameterizedTypeReference<>() {
                });

        List<CommonStreamResult<Object>> bodies = result.getResponseBody().collectList().block();
        assertThat(bodies).hasSize(1);
        CommonStreamResult<Object> body = bodies.get(0);
        assertThat(body.getSuccess()).isFalse();
        assertThat(body.getEvent()).isEqualTo("error");
        assertThat(body.getCode()).isEqualTo(400);
        assertThat(body.getMsg()).isEqualTo("消息内容不能为空");
    }

    private ObjectProvider<ExceptionMessageResolver> emptyExceptionMessageResolverProvider() {
        return new ObjectProvider<>() {
            @Override
            public ExceptionMessageResolver getObject(Object... args) {
                return null;
            }

            @Override
            public ExceptionMessageResolver getIfAvailable() {
                return null;
            }

            @Override
            public ExceptionMessageResolver getIfUnique() {
                return null;
            }

            @Override
            public ExceptionMessageResolver getObject() {
                return null;
            }
        };
    }

    private static class StubAiChatService implements IAiChatService {

        private Flux<CommonStreamResult<AiChatStreamEventVo>> streamResponse = Flux.empty();

        @Override
        public Long createConversation(AiConversationCreateDto createDto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResponse<AiConversationPageVo> pageConversations(AiConversationPageQueryDto queryDto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiConversationDetailVo getConversation(Long conversationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void renameConversation(Long conversationId, AiConversationTitleUpdateDto updateDto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteConversation(Long conversationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResponse<AiMessagePageVo> pageMessages(Long conversationId, AiMessagePageQueryDto queryDto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiChatSendVo sendMessage(Long conversationId, AiMessageSendDto sendDto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Flux<CommonStreamResult<AiChatStreamEventVo>> sendMessageStream(Long conversationId, AiMessageSendDto sendDto) {
            return streamResponse;
        }
    }
}
