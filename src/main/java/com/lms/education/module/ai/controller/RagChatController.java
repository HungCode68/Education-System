package com.lms.education.module.ai.controller;

import com.lms.education.module.ai.dto.ChatRequest;
import com.lms.education.module.ai.dto.ChatResponse;
import com.lms.education.module.ai.service.RagChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class RagChatController {

    private final RagChatService ragChatService;

    @PostMapping("/chat")
    @PreAuthorize("hasAnyAuthority('STUDENT_VIEW', 'STUDENT_CREATE', 'STUDENT_UPDATE')") // Adjust authorities as per
                                                                                         // your security model
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = ragChatService.chat(request);
        return ResponseEntity.ok(response);
    }
}
