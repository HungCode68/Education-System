package com.lms.education.module.ai.service;

import com.lms.education.exception.AiServiceUnavailableException;
import com.lms.education.module.ai.dto.AiDocumentIngestRequest;
import com.lms.education.module.ai.entity.AiDocument;
import com.lms.education.module.ai.repository.AiDocumentChunkRepository;
import com.lms.education.module.ai.repository.AiDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiIngestionService {

    private final AiDocumentRepository documentRepository;
    private final AiDocumentChunkRepository chunkRepository;
    private final EmbeddingModel embeddingModel;

    @Transactional
    public void ingestDocument(AiDocumentIngestRequest request) {
        // 1. Create and save Document record
        AiDocument document = AiDocument.builder()
                .kbId(request.getKbId())
                .materialId(request.getMaterialId())
                .title(request.getTitle())
                .processingStatus("PROCESSING")
                .build();
        document = documentRepository.save(document);

        // 2. Split content into chunks
        String[] rawChunks = request.getContent().split("\\n\\n");
        List<String> chunks = new ArrayList<>();
        
        StringBuilder currentChunk = new StringBuilder();
        int maxLength = 1000;
        
        for (String raw : rawChunks) {
            if (currentChunk.length() + raw.length() > maxLength && !currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(raw).append("\n\n");
        }
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        // 3. Embed and save chunks
        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = chunks.get(i);
            if (chunkContent.isBlank()) continue;

            float[] embedding;
            try {
                embedding = embeddingModel.embed(chunkContent);
            } catch (Exception e) {
                log.error("Failed to embed chunk {}: {}", i, e.getMessage());
                throw new AiServiceUnavailableException("Lỗi kết nối Embedding Model khi xử lý tài liệu", e);
            }
            
            String vectorStr = Arrays.toString(embedding);
            chunkRepository.insertChunk(document.getId(), i, chunkContent, vectorStr);
        }

        // 4. Update status
        document.setProcessingStatus("COMPLETED");
        documentRepository.save(document);
    }
}
