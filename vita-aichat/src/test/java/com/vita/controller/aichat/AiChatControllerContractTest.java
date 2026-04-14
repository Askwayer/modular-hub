package com.vita.controller.aichat;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AiChatControllerContractTest {

    @Test
    void shouldOnlyKeepFormalChatController() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.vita.controller.aichat.AiConversationController"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.vita.controller.aichat.AiMessageController"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.vita.controller.aichat.AiConversationSummaryController"));
    }

    @Test
    void chatControllerShouldExposeOnlyFormalRoutes() throws NoSuchMethodException {
        RequestMapping requestMapping = AiChatController.class.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping);
        assertArrayEquals(new String[]{"/chat"}, requestMapping.value());

        Set<String> methodNames = Arrays.stream(AiChatController.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "createConversation",
                "pageConversations",
                "getConversation",
                "renameConversation",
                "deleteConversation",
                "pageMessages",
                "sendMessage",
                "streamMessage"
        ), methodNames);

        assertPostMapping("createConversation", "/conversations");
        assertGetMapping("pageConversations", "/conversations");
        assertGetMapping("getConversation", "/conversations/{conversationId}");
        assertPatchMapping("renameConversation", "/conversations/{conversationId}/title");
        assertDeleteMapping("deleteConversation", "/conversations/{conversationId}");
        assertGetMapping("pageMessages", "/conversations/{conversationId}/messages");
        assertPostMapping("sendMessage", "/conversations/{conversationId}/messages");

        Method streamMethod = AiChatController.class.getDeclaredMethod(
                "streamMessage", Long.class, com.vita.aichat.chat.dto.AiChatMessageSendReqDto.class);
        PostMapping streamMapping = streamMethod.getAnnotation(PostMapping.class);
        assertNotNull(streamMapping);
        assertArrayEquals(new String[]{"/conversations/{conversationId}/messages/stream"}, streamMapping.value());
        assertArrayEquals(new String[]{MediaType.TEXT_EVENT_STREAM_VALUE}, streamMapping.produces());
    }

    private void assertGetMapping(String methodName, String path) throws NoSuchMethodException {
        Method method = findMethod(methodName);
        GetMapping annotation = method.getAnnotation(GetMapping.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[]{path}, annotation.value());
    }

    private void assertPostMapping(String methodName, String path) throws NoSuchMethodException {
        Method method = findMethod(methodName);
        PostMapping annotation = method.getAnnotation(PostMapping.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[]{path}, annotation.value());
    }

    private void assertPatchMapping(String methodName, String path) throws NoSuchMethodException {
        Method method = findMethod(methodName);
        PatchMapping annotation = method.getAnnotation(PatchMapping.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[]{path}, annotation.value());
    }

    private void assertDeleteMapping(String methodName, String path) throws NoSuchMethodException {
        Method method = findMethod(methodName);
        DeleteMapping annotation = method.getAnnotation(DeleteMapping.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[]{path}, annotation.value());
    }

    private Method findMethod(String methodName) throws NoSuchMethodException {
        return Arrays.stream(AiChatController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(methodName));
    }
}
