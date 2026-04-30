package com.vita.controller.aichat;

import com.vita.aichat.chat.dto.*;
import com.vita.aichat.chat.service.IAiChatService;
import com.vita.aichat.chat.support.AiChatReactiveBridge;
import com.vita.aichat.chat.support.AiChatReactiveHttpSupport;
import com.vita.aichat.chat.vo.*;
import com.vita.core.CommonResult;
import com.vita.core.CommonStreamResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * AI Chat 控制器。
 */
@Validated
@RestController
@RequestMapping("/chat")
public class AiChatController extends BaseController {

    private final IAiChatService aiChatService;
    private final AiChatReactiveBridge aiChatReactiveBridge;

    public AiChatController(IAiChatService aiChatService,
                            AiChatReactiveBridge aiChatReactiveBridge) {
        this.aiChatService = aiChatService;
        this.aiChatReactiveBridge = aiChatReactiveBridge;
    }

    /**
     * 创建会话。
     *
     * @param createDto 创建请求对象
     * @return 会话ID
     */
    @PostMapping("/conversations")
    public Mono<CommonResult<Long>> createConversation(@RequestBody @Valid AiConversationCreateDto createDto) {
        return blockingCall(contextView -> aiChatService.createConversation(createDto))
                .map(CommonResult::success);
    }

    /**
     * 分页查询会话列表。
     *
     * @param queryDto 分页查询条件
     * @return 会话分页数据
     */
    @GetMapping("/conversations")
    public Mono<CommonResult<PageResponse<AiConversationPageVo>>> pageConversations(@Valid AiConversationPageQueryDto queryDto) {
        return blockingCall(contextView -> aiChatService.pageConversations(queryDto))
                .map(CommonResult::success);
    }

    /**
     * 查询会话详情。
     *
     * @param conversationId 会话ID
     * @return 会话详情对象
     */
    @GetMapping("/conversations/{conversationId}")
    public Mono<CommonResult<AiConversationDetailVo>> getConversation(@PathVariable("conversationId") Long conversationId) {
        return blockingCall(contextView -> aiChatService.getConversation(conversationId))
                .map(CommonResult::success);
    }

    /**
     * 重命名会话标题。
     *
     * @param conversationId 会话ID
     * @param updateDto      标题更新请求对象
     * @return 操作结果
     */
    @PostMapping("/conversations/{conversationId}/title")
    public Mono<CommonResult<Boolean>> renameConversation(@PathVariable("conversationId") Long conversationId,
                                                          @RequestBody @Valid AiConversationTitleUpdateDto updateDto) {
        return blockingCall(contextView -> {
            aiChatService.renameConversation(conversationId, updateDto);
            return Boolean.TRUE;
        }).map(CommonResult::success);
    }

    /**
     * 删除会话。
     *
     * @param conversationId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/conversations/{conversationId}")
    public Mono<CommonResult<Boolean>> deleteConversation(@PathVariable("conversationId") Long conversationId) {
        return blockingCall(contextView -> {
            aiChatService.deleteConversation(conversationId);
            return Boolean.TRUE;
        }).map(CommonResult::success);
    }

    /**
     * 分页查询消息列表。
     *
     * @param conversationId 会话ID
     * @param queryDto       分页查询条件
     * @return 消息分页数据
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public Mono<CommonResult<PageResponse<AiMessagePageVo>>> pageMessages(@PathVariable("conversationId") Long conversationId,
                                                                          @Valid AiMessagePageQueryDto queryDto) {
        return blockingCall(contextView -> aiChatService.pageMessages(conversationId, queryDto))
                .map(CommonResult::success);
    }

    /**
     * 发送消息（同步模式）。
     *
     * @param conversationId 会话ID
     * @param sendDto        消息发送请求对象
     * @return 发送结果
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public Mono<CommonResult<AiChatSendVo>> sendMessage(@PathVariable("conversationId") Long conversationId,
                                                        @RequestBody @Valid AiMessageSendDto sendDto) {
        return blockingCall(contextView -> aiChatService.sendMessage(conversationId, sendDto))
                .map(CommonResult::success);
    }

    /**
     * 发送消息（流式模式）。
     *
     * @param conversationId 会话ID
     * @param sendDto        消息发送请求对象
     * @return NDJSON 流式响应
     */
    @PostMapping(value = "/conversations/{conversationId}/messages/stream", produces = AiChatReactiveHttpSupport.NDJSON_CONTENT_TYPE_VALUE)
    public Flux<CommonStreamResult<AiChatStreamEventVo>> sendMessageStream(@PathVariable("conversationId") Long conversationId,
                                                                           @RequestBody @Valid AiMessageSendDto sendDto) {
        return aiChatService.sendMessageStream(conversationId, sendDto);
    }

    private <T> Mono<T> blockingCall(BlockingSupplier<T> supplier) {
        return Mono.deferContextual(contextView ->
                aiChatReactiveBridge.blocking(aiChatReactiveBridge.currentRequestContext(contextView),
                        () -> supplier.get(contextView)));
    }

    @FunctionalInterface
    private interface BlockingSupplier<T> {

        T get(ContextView contextView) throws Exception;
    }
}
