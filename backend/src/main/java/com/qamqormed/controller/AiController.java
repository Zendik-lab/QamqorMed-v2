package com.qamqormed.controller;

import com.qamqormed.dto.AiChatRequest;
import com.qamqormed.dto.AiChatResponse;
import com.qamqormed.dto.AiConversationResponse;
import com.qamqormed.dto.AiMessageResponse;
import com.qamqormed.dto.AiAppointmentProposalResponse;
import com.qamqormed.dto.AiPrepareAppointmentRequest;
import com.qamqormed.dto.AiPrepareWaitlistRequest;
import com.qamqormed.dto.AiWaitlistProposalResponse;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.AiBookingService;
import com.qamqormed.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final AiBookingService aiBookingService;
    private final com.qamqormed.service.AiToolService aiToolService;

    public AiController(AiService aiService, AiBookingService aiBookingService,
                        com.qamqormed.service.AiToolService aiToolService) {
        this.aiService = aiService;
        this.aiBookingService = aiBookingService;
        this.aiToolService = aiToolService;
    }

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

    @GetMapping("/doctors/{doctorId}/slots")
    public com.qamqormed.dto.AiAvailableSlotsResponse availableSlots(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long doctorId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "timeFrom", required = false) String timeFrom,
            @RequestParam(value = "timeTo", required = false) String timeTo) {
        return aiToolService.availableSlotsForUi(doctorId, date, timeFrom, timeTo);
    }

    @PostMapping("/appointments/proposals")
    public AiAppointmentProposalResponse prepareAppointment(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @Valid @RequestBody AiPrepareAppointmentRequest request) {
        return aiBookingService.prepareAppointment(user.getId(), request);
    }

    @PostMapping("/appointments/proposals/{proposalId}/confirm")
    public AiAppointmentProposalResponse confirmAppointment(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @PathVariable Long proposalId) {
        return aiBookingService.confirmAppointment(user.getId(), proposalId);
    }

    @PostMapping("/appointments/proposals/{proposalId}/cancel")
    public ResponseEntity<Void> cancelAppointment(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @PathVariable Long proposalId) {
        aiBookingService.cancelAppointment(user.getId(), proposalId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/waitlist/proposals")
    public AiWaitlistProposalResponse prepareWaitlist(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @Valid @RequestBody AiPrepareWaitlistRequest request) {
        return aiBookingService.prepareWaitlist(user.getId(), request);
    }

    @PostMapping("/waitlist/proposals/{proposalId}/confirm")
    public AiWaitlistProposalResponse confirmWaitlist(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @PathVariable Long proposalId) {
        return aiBookingService.confirmWaitlist(user.getId(), proposalId);
    }

    @PostMapping("/waitlist/proposals/{proposalId}/cancel")
    public ResponseEntity<Void> cancelWaitlist(@AuthenticationPrincipal AuthenticatedUser user,
                                               @PathVariable Long proposalId) {
        aiBookingService.cancelWaitlist(user.getId(), proposalId);
        return ResponseEntity.noContent().build();
    }
}
