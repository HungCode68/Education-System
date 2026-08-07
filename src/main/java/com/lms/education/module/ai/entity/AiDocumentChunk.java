package com.lms.education.module.ai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_document_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiDocumentChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // Do NOT map the 'embedding' column here because JPA doesn't support MySQL native VECTOR.
    // It will be queried via native SQL.
}
