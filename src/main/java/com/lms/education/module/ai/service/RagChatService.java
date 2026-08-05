package com.lms.education.module.ai.service;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.ai.dto.ChatRequest;
import com.lms.education.module.ai.dto.ChatResponse;
import com.lms.education.module.ai.dto.ChatSourceDto;
import com.lms.education.module.ai.entity.AiChatMessage;
import com.lms.education.module.ai.entity.AiChatSession;
import com.lms.education.module.ai.repository.AiChatMessageRepository;
import com.lms.education.module.ai.repository.AiChatSessionRepository;
import com.lms.education.module.ai.repository.AiDocumentChunkRepository;
import com.lms.education.module.ai.repository.ChunkSearchProjection;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.repository.StudentRepository;
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
    public ChatResponse chat(ChatRequest request) {
        // 1. Lấy thông tin học viên (System Prompt Context)
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên với ID: " + request.getStudentId()));

        String systemPromptText = buildSystemPrompt(student);

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
        if (request.getSessionId() != null) {
            session = sessionRepository.findById(request.getSessionId())
                    .orElseGet(() -> createNewSession(student.getId(), request.getMessage()));
        } else {
            session = createNewSession(student.getId(), request.getMessage());
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

    private AiChatSession createNewSession(Long studentId, String title) {
        // Lấy 50 ký tự đầu làm title
        String sessionTitle = title.length() > 50 ? title.substring(0, 50) + "..." : title;
        AiChatSession newSession = AiChatSession.builder()
                .studentId(studentId)
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
}
