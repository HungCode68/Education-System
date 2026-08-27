package com.lms.education.module.ai.service;

import com.lms.education.exception.AiServiceUnavailableException;
import com.lms.education.module.ai.dto.AiDocumentIngestRequest;
import com.lms.education.module.ai.entity.AiDocument;
import com.lms.education.module.ai.repository.AiDocumentChunkRepository;
import com.lms.education.module.ai.repository.AiDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.embedding.EmbeddingModel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AiIngestionServiceTest {

    @Mock
    private AiDocumentRepository documentRepository;
    @Mock
    private AiDocumentChunkRepository chunkRepository;
    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private AiIngestionService aiIngestionService;

    private AiDocumentIngestRequest request;
    private AiDocument mockDocument;

    @BeforeEach
    void setUp() {
        request = AiDocumentIngestRequest.builder()
                .kbId(1L)
                .materialId(100L)
                .title("Test Document")
                .content("Paragraph 1.\n\nParagraph 2.\n\nParagraph 3.")
                .build();

        mockDocument = new AiDocument();
        mockDocument.setId(10L);
        mockDocument.setKbId(1L);
        mockDocument.setProcessingStatus("PROCESSING");
    }

    @Test
    void ingestDocument_Success() {
        when(documentRepository.save(any(AiDocument.class))).thenReturn(mockDocument);
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        
        doNothing().when(chunkRepository).insertChunk(anyLong(), anyInt(), anyString(), anyString());

        assertDoesNotThrow(() -> aiIngestionService.ingestDocument(request));

        verify(documentRepository, times(2)).save(any(AiDocument.class));
        verify(chunkRepository, times(1)).insertChunk(anyLong(), anyInt(), anyString(), anyString());
        assertEquals("COMPLETED", mockDocument.getProcessingStatus());
    }

    @Test
    void ingestDocument_EmbeddingFails_ThrowsException() {
        when(documentRepository.save(any(AiDocument.class))).thenReturn(mockDocument);
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("API Error"));

        assertThrows(AiServiceUnavailableException.class, () -> aiIngestionService.ingestDocument(request));
    }
}
