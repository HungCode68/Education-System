package com.lms.education.module.ai.repository;

public interface ChunkSearchProjection {
    Long getId();
    Long getDocumentId();
    Integer getChunkIndex();
    String getContent();
    String getEmbeddingStr();
    String getTitle();
}
