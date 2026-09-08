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
        List<String> chunks = new ArrayList<>();
        int maxLength = 1000;
        String content = request.getContent();
        
        // Split by double newline first
        String[] rawParagraphs = content.split("\\n\\n");
        StringBuilder currentChunk = new StringBuilder();
        
        for (String para : rawParagraphs) {
            String trimmedPara = para.trim();
            if (trimmedPara.isEmpty()) continue;
            
            // If the paragraph itself is larger than maxLength, we need to hard split it
            if (trimmedPara.length() > maxLength) {
                // First, save the current chunk if it has content
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                
                // Hard split the large paragraph
                int startIndex = 0;
                while (startIndex < trimmedPara.length()) {
                    int endIndex = Math.min(startIndex + maxLength, trimmedPara.length());
                    chunks.add(trimmedPara.substring(startIndex, endIndex));
                    startIndex = endIndex;
                }
            } else {
                if (currentChunk.length() + trimmedPara.length() > maxLength) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                currentChunk.append(trimmedPara).append("\n\n");
            }
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

    @Transactional
    public void deleteDocumentByMaterialId(Long materialId) {
        AiDocument document = documentRepository.findByMaterialId(materialId).orElse(null);
        if (document != null) {
            chunkRepository.deleteByDocumentId(document.getId());
            documentRepository.delete(document);
        }
    }
}
