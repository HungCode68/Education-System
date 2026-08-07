package com.lms.education.module.ai.controller;

import com.lms.education.module.ai.dto.AiDocumentIngestRequest;
import com.lms.education.module.ai.service.AiIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/documents")
@RequiredArgsConstructor
public class AiIngestionController {

    private final AiIngestionService aiIngestionService;

    @PostMapping("/ingest")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR_VIEW', 'INSTRUCTOR_CREATE', 'INSTRUCTOR_UPDATE', 'ADMIN')") 
    public ResponseEntity<String> ingestDocument(@Valid @RequestBody AiDocumentIngestRequest request) {
        aiIngestionService.ingestDocument(request);
        return ResponseEntity.ok("Ingestion process started and completed successfully.");
    }
}
