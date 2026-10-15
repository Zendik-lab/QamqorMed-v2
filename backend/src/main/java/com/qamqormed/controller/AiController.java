package com.qamqormed.controller;

import com.qamqormed.dto.AiChatRequest;
import com.qamqormed.dto.AiChatResponse;
import com.qamqormed.dto.AiConversationResponse;
import com.qamqormed.dto.AiMessageResponse;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) { this.aiService = aiService; }

    @GetMapping("/conversations")
    public List<AiConversationResponse> conversations(@AuthenticationPrincipal AuthenticatedUser user) {
        return aiService.listConversations(user.getId());
    }

    @PostMapping("/conversations")
    public ResponseEntity<AiConversationResponse> createConversation(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(aiService.createConversation(user.getId(), user.getRole()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<AiMessageResponse> messages(@AuthenticationPrincipal AuthenticatedUser user,
                                            @PathVariable Long conversationId) {
        return aiService.listMessages(user.getId(), conversationId);
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @PathVariable Long conversationId) {
        aiService.deleteConversation(user.getId(), conversationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/chat")
    public CompletableFuture<AiChatResponse> chat(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @Valid @RequestBody AiChatRequest request) {
        return aiService.chat(user.getId(), user.getRole(), request);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @Valid @RequestBody AiChatRequest request) {
        aiService.ensureEnabled();
        StreamingResponseBody body = outputStream -> {
            try {
                aiService.stream(user.getId(), user.getRole(), request, outputStream);
            } catch (IOException exception) {
                // The browser may cancel generation. Do not expose transport details or log message content.
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache, no-transform")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }
}
