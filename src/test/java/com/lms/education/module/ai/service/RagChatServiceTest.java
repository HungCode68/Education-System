package com.lms.education.module.ai.service;

import com.lms.education.exception.AiServiceUnavailableException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.ai.dto.ChatRequest;
import com.lms.education.module.ai.dto.ChatResponse;
import com.lms.education.module.ai.entity.AiChatMessage;
import com.lms.education.module.ai.entity.AiChatSession;
import com.lms.education.module.ai.repository.AiChatMessageRepository;
import com.lms.education.module.ai.repository.AiChatSessionRepository;
import com.lms.education.module.ai.repository.AiDocumentChunkRepository;
import com.lms.education.module.ai.repository.ChunkSearchProjection;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RagChatServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AiDocumentChunkRepository chunkRepository;
    @Mock
    private AiChatSessionRepository sessionRepository;
    @Mock
    private AiChatMessageRepository messageRepository;
    @Mock
    private ChatModel chatModel;
    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private RagChatService ragChatService;

    private User mockUser;
    private Student mockStudent;
    private AiChatSession mockSession;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ragChatService, "similarityThreshold", 0.6);
        ReflectionTestUtils.setField(ragChatService, "topK", 3);

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("student@test.com");

        mockStudent = new Student();
        mockStudent.setId(10L);
        mockStudent.setUser(mockUser);
        mockStudent.setFullName("Test Student");
        mockStudent.setStudentCode("STU01");

        mockSession = new AiChatSession();
        mockSession.setId(100L);
        mockSession.setUserId(1L);
        mockSession.setTitle("Test Session");
    }

    @Test
    void chat_Success_WithStudentUser_WithRelevantChunks() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello AI");
        request.setSessionId(100L);

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(mockUser));
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.of(mockStudent));
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{1.0f, 0.0f});

        ChunkSearchProjection projection = new ChunkSearchProjection() {
            @Override public Long getId() { return 1L; }
            @Override public Long getDocumentId() { return 10L; }
            @Override public String getTitle() { return "Doc1"; }
            @Override public Integer getChunkIndex() { return 0; }
            @Override public String getContent() { return "Relevant context"; }
            @Override public String getEmbeddingStr() { return "[1.0, 0.0]"; } // 1.0 cosine similarity
        };
        when(chunkRepository.findAllWithEmbeddings(2000)).thenReturn(List.of(projection));

        org.springframework.ai.chat.model.ChatResponse mockResponse = mock(org.springframework.ai.chat.model.ChatResponse.class, RETURNS_DEEP_STUBS);
        when(mockResponse.getResult().getOutput().getText()).thenReturn("AI Reply");
        when(mockResponse.getMetadata().getUsage()).thenReturn(null);
        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(mockResponse);

        when(sessionRepository.findById(100L)).thenReturn(Optional.of(mockSession));
        when(messageRepository.save(any(AiChatMessage.class))).thenAnswer(i -> i.getArgument(0));

        ChatResponse response = ragChatService.chat(request, "student@test.com");

        assertNotNull(response);
        assertEquals("AI Reply", response.getAnswer());
        assertEquals(1, response.getSources().size());
        verify(messageRepository, times(2)).save(any(AiChatMessage.class));
    }

    @Test
    void chat_Success_WithGenericUser_NoChunks() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");

        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.empty()); // Not a student
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{1.0f, 0.0f});

        ChunkSearchProjection projection = new ChunkSearchProjection() {
            @Override public Long getId() { return 1L; }
            @Override public Long getDocumentId() { return 10L; }
            @Override public String getTitle() { return "Doc1"; }
            @Override public Integer getChunkIndex() { return 0; }
            @Override public String getContent() { return "Irrelevant context"; }
            @Override public String getEmbeddingStr() { return "[-1.0, 0.0]"; } // -1.0 cosine similarity
        };
        when(chunkRepository.findAllWithEmbeddings(2000)).thenReturn(List.of(projection));

        org.springframework.ai.chat.model.ChatResponse mockResponse = mock(org.springframework.ai.chat.model.ChatResponse.class, RETURNS_DEEP_STUBS);
        when(mockResponse.getResult().getOutput().getText()).thenReturn("Generic AI Reply");
        when(mockResponse.getMetadata().getUsage()).thenReturn(null);
        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(mockResponse);

        when(sessionRepository.save(any(AiChatSession.class))).thenReturn(mockSession);

        ChatResponse response = ragChatService.chat(request, "teacher@test.com");

        assertNotNull(response);
        assertEquals("Generic AI Reply", response.getAnswer());
        assertEquals(0, response.getSources().size()); // No relevant chunks passed threshold
    }

    @Test
    void chat_EmbeddingFails_ThrowsException() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("API Error"));

        assertThrows(AiServiceUnavailableException.class, () -> ragChatService.chat(request, null));
    }

    @Test
    void chat_ChatModelFails_ThrowsException() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{1.0f});
        when(chunkRepository.findAllWithEmbeddings(2000)).thenReturn(List.of());
        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenThrow(new RuntimeException("API Error"));

        assertThrows(AiServiceUnavailableException.class, () -> ragChatService.chat(request, null));
    }
    
    @Test
    void getSessionMessages_Success() {
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(mockSession));
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(mockUser));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(new AiChatMessage()));
        
        var messages = ragChatService.getSessionMessages(100L, "student@test.com");
        
        assertEquals(1, messages.size());
    }
    
    @Test
    void getSessionMessages_NotPermitted_ThrowsException() {
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(mockSession));
        User otherUser = new User();
        otherUser.setId(2L);
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherUser));
        
        assertThrows(OperationNotPermittedException.class, () -> ragChatService.getSessionMessages(100L, "other@test.com"));
    }

    @Test
    void deleteSession_Success() {
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(mockSession));
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(mockUser));
        
        ragChatService.deleteSession(100L, "student@test.com");
        
        verify(sessionRepository).delete(mockSession);
    }
}
