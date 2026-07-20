-- ==============================================================================
-- Migration Script: Cập nhật lại cấu trúc bảng learning_materials theo thiết kế mới
-- ==============================================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS learning_materials;

CREATE TABLE IF NOT EXISTS learning_materials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL, -- Tiêu đề tài liệu
    material_type VARCHAR(50) NOT NULL, -- DOCUMENT, SLIDE, VIDEO, IMAGE, EXTERNAL_LINK
    source_type VARCHAR(50) DEFAULT 'MINIO', -- MINIO, EXTERNAL
    resource_url TEXT NOT NULL, -- Đường dẫn Object trên MinIO hoặc link ngoài
    file_size BIGINT NULL, -- Dung lượng file (bytes), NULL nếu là EXTERNAL_LINK
    display_order INT DEFAULT 0, -- Thứ tự sắp xếp hiển thị
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lm_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);

SET FOREIGN_KEY_CHECKS = 1;
