-- ==============================================================================
-- Migration Script: Bổ sung start_date và end_date cho bảng teaching_substitutions
-- ==============================================================================

ALTER TABLE teaching_substitutions
    ADD COLUMN start_date DATE,
    ADD COLUMN end_date DATE;

-- Cập nhật ngày mặc định (hôm nay) cho các bản ghi cũ
UPDATE teaching_substitutions SET start_date = CURRENT_DATE, end_date = CURRENT_DATE WHERE start_date IS NULL;

-- Áp dụng ràng buộc NOT NULL sau khi đã điền dữ liệu
ALTER TABLE teaching_substitutions
    MODIFY COLUMN start_date DATE NOT NULL,
    MODIFY COLUMN end_date DATE NOT NULL;
