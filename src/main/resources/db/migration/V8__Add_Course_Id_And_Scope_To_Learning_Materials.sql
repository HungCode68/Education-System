-- ==============================================================================
-- Migration Script: Bổ sung course_id và material_scope cho bảng learning_materials
-- Hỗ trợ lưu trữ tài liệu thuộc cấp độ Khóa học (COURSE) hoặc Bài học (LESSON)
-- ==============================================================================

ALTER TABLE learning_materials
    MODIFY COLUMN lesson_id BIGINT NULL,
    ADD COLUMN course_id BIGINT NULL AFTER lesson_id,
    ADD COLUMN material_scope VARCHAR(20) NOT NULL DEFAULT 'LESSON' AFTER course_id,
    ADD CONSTRAINT fk_lm_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE;

-- Đảm bảo tất cả dữ liệu cũ được thiết lập material_scope là LESSON
UPDATE learning_materials
SET material_scope = 'LESSON'
WHERE lesson_id IS NOT NULL;

-- Thêm ràng buộc kiểm tra duy nhất một phạm vi (COURSE hoặc LESSON)
ALTER TABLE learning_materials
ADD CONSTRAINT chk_exclusive_material_scope 
CHECK (
    (course_id IS NOT NULL AND lesson_id IS NULL AND material_scope = 'COURSE') 
    OR 
    (course_id IS NULL AND lesson_id IS NOT NULL AND material_scope = 'LESSON')
);
