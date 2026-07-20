-- ==============================================================================
-- Migration Script: Tạo bảng schedule_assignments để phân công giáo viên vào ca học
-- ==============================================================================

CREATE TABLE IF NOT EXISTS schedule_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    role VARCHAR(50) DEFAULT 'MAIN_TEACHER', -- MAIN_TEACHER, NATIVE_TEACHER, ASSISTANT
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sa_schedule FOREIGN KEY (schedule_id) REFERENCES class_schedules(id) ON DELETE CASCADE,
    CONSTRAINT fk_sa_staff FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE CASCADE,
    -- Giữ tính toàn vẹn: 1 Giáo viên không được gán 2 lần vào cùng 1 ca học
    UNIQUE KEY uk_schedule_staff (schedule_id, staff_id)
);
