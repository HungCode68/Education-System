package com.lms.education.module.ai.controller;

import com.lms.education.module.ai.dto.ChatRequest;
import com.lms.education.module.ai.dto.ChatResponse;
import com.lms.education.module.ai.dto.AiChatSessionDto;
import com.lms.education.module.ai.dto.AiChatMessageDto;
import com.lms.education.module.ai.service.RagChatService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class RagChatController {

    private final RagChatService ragChatService;

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()") // Allow all authenticated users (Students, Teachers, Admins, etc.)
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = null;
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            userEmail = ((UserDetails) auth.getPrincipal()).getUsername();
        }

        ChatResponse response = ragChatService.chat(request, userEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/chat/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AiChatSessionDto>> getUserChatSessions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = null;
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            userEmail = ((UserDetails) auth.getPrincipal()).getUsername();
        }

        List<AiChatSessionDto> sessions = ragChatService.getUserChatSessions(userEmail);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/chat/sessions/{sessionId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AiChatMessageDto>> getSessionMessages(@PathVariable Long sessionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = null;
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            userEmail = ((UserDetails) auth.getPrincipal()).getUsername();
        }

        List<AiChatMessageDto> messages = ragChatService.getSessionMessages(sessionId, userEmail);
        return ResponseEntity.ok(messages);
    }
    @DeleteMapping("/chat/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = null;
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            userEmail = ((UserDetails) auth.getPrincipal()).getUsername();
        }

        ragChatService.deleteSession(sessionId, userEmail);
        return ResponseEntity.noContent().build();
    }
}
