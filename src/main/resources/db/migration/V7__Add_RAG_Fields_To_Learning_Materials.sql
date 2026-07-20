-- ==============================================================================
-- Migration Script: Bổ sung các trường RAG & Người tải lên cho bảng learning_materials
-- ==============================================================================

ALTER TABLE learning_materials
    ADD COLUMN is_official BOOLEAN DEFAULT FALSE,
    ADD COLUMN is_rag_enabled BOOLEAN DEFAULT FALSE,
    ADD COLUMN indexing_status VARCHAR(50) DEFAULT 'NOT_INDEXED',
    ADD COLUMN uploaded_by BIGINT NULL,
    ADD CONSTRAINT fk_lm_user FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL;
