package com.lms.education.module.ai.repository;

import com.lms.education.module.ai.entity.AiDocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiDocumentChunkRepository extends JpaRepository<AiDocumentChunk, Long> {

    // TODO: Sau này thay bằng scope theo enrollment của học viên để giới hạn số lượng tài liệu load lên.
    // Tạm thời query toàn bộ (với LIMIT cận an toàn) và lấy title từ ai_documents.
    // Tính toán similarity sẽ được thực hiện ở Java layer do MySQL Community Edition không hỗ trợ hàm DISTANCE.
    @Query(value = """
            SELECT
                c.id as id,
                c.document_id as documentId,
                c.chunk_index as chunkIndex,
                c.content as content,
                d.title as title,
                VECTOR_TO_STRING(c.embedding) as embeddingStr
            FROM ai_document_chunks c
            LEFT JOIN ai_documents d ON c.document_id = d.id
            LIMIT :maxCandidates
            """, nativeQuery = true)
    List<ChunkSearchProjection> findAllWithEmbeddings(@Param("maxCandidates") int maxCandidates);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
            INSERT INTO ai_document_chunks (document_id, chunk_index, content, embedding)
            VALUES (:documentId, :chunkIndex, :content, STRING_TO_VECTOR(:vectorStr))
            """, nativeQuery = true)
    void insertChunk(
            @Param("documentId") Long documentId,
            @Param("chunkIndex") Integer chunkIndex,
            @Param("content") String content,
            @Param("vectorStr") String vectorStr);
}
