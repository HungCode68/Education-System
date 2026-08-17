package com.lms.education.module.ai.service;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.ai.dto.ChatRequest;
import com.lms.education.module.ai.dto.ChatResponse;
import com.lms.education.module.ai.dto.ChatSourceDto;
import com.lms.education.module.ai.dto.AiChatSessionDto;
import com.lms.education.module.ai.dto.AiChatMessageDto;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AiDocumentChunkRepository chunkRepository;
    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;

    @Value("${rag.similarity-threshold:0.6}")
    private double similarityThreshold;

    @Value("${rag.top-k:3}")
    private int topK;

    @Transactional
    public ChatResponse chat(ChatRequest request, String userEmail) {
        // 1. Xác định User và sinh System Prompt (Dynamic Prompt)
        User currentUser = null;
        Long userId = null;
        if (userEmail != null) {
            currentUser = userRepository.findByEmail(userEmail).orElse(null);
            if (currentUser != null) {
                userId = currentUser.getId();
            }
        }

        String systemPromptText;
        if (userId != null) {
            Student student = studentRepository.findByUserId(userId).orElse(null);
            if (student != null) {
                // Nếu là Học viên -> Dùng prompt riêng cho Học viên (Giữ nguyên logic cũ)
                systemPromptText = buildSystemPrompt(student);
            } else {
                // Nếu không phải Học viên (Quản lý, Giảng viên) -> Dùng prompt chung
                systemPromptText = buildGenericSystemPrompt();
            }
        } else {
            // Fallback (Trường hợp gọi API không qua login context, ví dụ test nội bộ)
            systemPromptText = buildGenericSystemPrompt();
        }

        long startTime = System.currentTimeMillis();
        
        // 2. Vector hóa câu hỏi người dùng
        long embedStartTime = System.currentTimeMillis();
        float[] queryEmbedding;
        try {
            queryEmbedding = embeddingModel.embed(request.getMessage());
        } catch (Exception e) {
            log.error("Error calling Embedding Model: ", e);
            throw new com.lms.education.exception.AiServiceUnavailableException("Service nhúng văn bản đang tạm thời không khả dụng.", e);
        }
        long embedEndTime = System.currentTimeMillis();
        log.info("TIME_LOG: embeddingModel.embed() took {} ms", (embedEndTime - embedStartTime));

        // 3. Vector Search trên database (Tính similarity phía Java)
        long searchStartTime = System.currentTimeMillis();
        List<ChunkSearchProjection> allCandidates = chunkRepository.findAllWithEmbeddings(2000);
        
        class RankedChunk {
            ChunkSearchProjection chunk;
            double similarity;
            RankedChunk(ChunkSearchProjection chunk, double similarity) {
                this.chunk = chunk;
                this.similarity = similarity;
            }
        }

        List<RankedChunk> relevantChunks = allCandidates.stream()
                .map(chunk -> {
                    float[] chunkEmbedding = com.lms.education.module.ai.util.VectorSimilarityUtils.parseVectorString(chunk.getEmbeddingStr());
                    double similarity = com.lms.education.module.ai.util.VectorSimilarityUtils.cosineSimilarity(queryEmbedding, chunkEmbedding);
                    return new RankedChunk(chunk, similarity);
                })
                .filter(rc -> rc.similarity >= similarityThreshold)
                .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                .limit(topK)
                .collect(Collectors.toList());
        long searchEndTime = System.currentTimeMillis();
        log.info("TIME_LOG: vector search & cosine similarity took {} ms (Candidates: {})", (searchEndTime - searchStartTime), allCandidates.size());

        // 4. Mode Switching (Dual-Context Strategy)
        String userPromptText = request.getMessage();
        List<ChatSourceDto> sources = new ArrayList<>();

        if (!relevantChunks.isEmpty()) {
            // Có ngữ cảnh tài liệu -> Đưa vào prompt
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("Dưới đây là thông tin tài liệu tham khảo:\n\n");
            
            for (RankedChunk rc : relevantChunks) {
                contextBuilder.append("- ").append(rc.chunk.getContent()).append("\n");
                
                // Add to sources DTO
                sources.add(ChatSourceDto.builder()
                        .chunkId(rc.chunk.getId())
                        .documentId(rc.chunk.getDocumentId())
                        .title(rc.chunk.getTitle())
                        .chunkIndex(rc.chunk.getChunkIndex())
                        .similarityScore(rc.similarity)
                        .build());
            }

            contextBuilder.append("\nCâu hỏi của người dùng: ").append(request.getMessage());
            contextBuilder.append("\n\nYêu cầu: Hãy trả lời dựa trên ngữ cảnh tài liệu tham khảo trên. Nếu không đủ thông tin từ tài liệu, hãy nói rõ là không có thông tin.");
            
            userPromptText = contextBuilder.toString();
        }

        // 5. Gọi AI Chat Model
        SystemMessage systemMessage = new SystemMessage(systemPromptText);
        UserMessage userMessage = new UserMessage(userPromptText);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        long chatStartTime = System.currentTimeMillis();
        org.springframework.ai.chat.model.ChatResponse aiResponse;
        try {
            aiResponse = chatModel.call(prompt);
        } catch (Exception e) {
            log.error("Error calling Chat Model: ", e);
            throw new com.lms.education.exception.AiServiceUnavailableException("AI Chat Model đang tạm thời không khả dụng.", e);
        }
        long chatEndTime = System.currentTimeMillis();
        log.info("TIME_LOG: chatModel.call() took {} ms", (chatEndTime - chatStartTime));
        
        String answer = aiResponse.getResult().getOutput().getText();

        // Metadata tokens (Ollama might not support complete usage info depending on the config, default to 0 if null)
        int promptTokens = aiResponse.getMetadata().getUsage() != null ? (int) aiResponse.getMetadata().getUsage().getTotalTokens() : 0;
        int completionTokens = 0;

        // 6. Lưu trữ Session & Message
        AiChatSession session;
        final Long finalUserId = userId;
        if (request.getSessionId() != null) {
            session = sessionRepository.findById(request.getSessionId())
                    .orElseGet(() -> createNewSession(finalUserId, request.getMessage()));
        } else {
            session = createNewSession(finalUserId, request.getMessage());
        }

        // Lưu User Message
        AiChatMessage userChatMsg = AiChatMessage.builder()
                .sessionId(session.getId())
                .role("USER")
                .content(request.getMessage())
                .build();
        messageRepository.save(userChatMsg);

        // Lưu Assistant Message
        AiChatMessage assistantChatMsg = AiChatMessage.builder()
                .sessionId(session.getId())
                .role("ASSISTANT")
                .content(answer)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .build();
        messageRepository.save(assistantChatMsg);

        long totalEndTime = System.currentTimeMillis();
        log.info("TIME_LOG: Total request time {} ms", (totalEndTime - startTime));

        return ChatResponse.builder()
                .sessionId(session.getId())
                .answer(answer)
                .sources(sources)
                .build();
    }

    private AiChatSession createNewSession(Long userId, String title) {
        // Lấy 50 ký tự đầu làm title
        String sessionTitle = title.length() > 50 ? title.substring(0, 50) + "..." : title;
        AiChatSession newSession = AiChatSession.builder()
                .userId(userId) // Có thể null nếu fallback
                .title(sessionTitle)
                .status("ACTIVE")
                .build();
        return sessionRepository.save(newSession);
    }

    private String buildSystemPrompt(Student student) {
        return String.format(
            "Bạn là trợ lý ảo giáo dục thông minh của hệ thống LMS. " +
            "Bạn đang nói chuyện với học viên: %s (Mã HV: %s). " +
            "Mục tiêu học tập của học viên là: %s. Trạng thái hiện tại: %s. " +
            "Hãy xưng hô phù hợp và hỗ trợ học viên một cách tốt nhất.",
            student.getFullName(),
            student.getStudentCode(),
            student.getTargetScore() != null ? student.getTargetScore() : "chưa xác định",
            student.getStatus()
        );
    }

    private String buildGenericSystemPrompt() {
        return "Bạn là trợ lý ảo giáo dục thông minh của hệ thống LMS. " +
               "Nhiệm vụ của bạn là giải đáp thắc mắc về kiến thức học thuật, tài liệu và lộ trình học tập cho người dùng một cách chính xác và thân thiện.";
    }

    @Transactional(readOnly = true)
    public List<AiChatSessionDto> getUserChatSessions(String userEmail) {
        Long userId = null;
        if (userEmail != null) {
            User currentUser = userRepository.findByEmail(userEmail).orElse(null);
            if (currentUser != null) {
                userId = currentUser.getId();
            }
        }
        
        if (userId == null) {
            return List.of();
        }

        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(session -> AiChatSessionDto.builder()
                        .id(session.getId())
                        .title(session.getTitle())
                        .status(session.getStatus())
                        .updatedAt(session.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AiChatMessageDto> getSessionMessages(Long sessionId, String userEmail) {
        // Validate access
        AiChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));
                
        Long userId = null;
        if (userEmail != null) {
            User currentUser = userRepository.findByEmail(userEmail).orElse(null);
            if (currentUser != null) {
                userId = currentUser.getId();
            }
        }
        
        if (userId == null || !userId.equals(session.getUserId())) {
            // User not authorized to view this session
            throw new com.lms.education.exception.OperationNotPermittedException("You don't have access to this chat session");
        }

        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(msg -> AiChatMessageDto.builder()
                        .id(msg.getId())
                        .sessionId(msg.getSessionId())
                        .role("ASSISTANT".equalsIgnoreCase(msg.getRole()) ? "AI" : msg.getRole())
                        .content(msg.getContent())
                        .createdAt(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
