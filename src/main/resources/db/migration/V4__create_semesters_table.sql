-- Tạo bảng semesters
CREATE TABLE semesters (
                           id VARCHAR(36) PRIMARY KEY,
                           school_year_id VARCHAR(36) NOT NULL,
                           name VARCHAR(100) NOT NULL COMMENT 'Tên học kỳ (VD: Học kỳ 1, Học kỳ 2)',
                           code VARCHAR(20) COMMENT 'Mã học kỳ (VD: HK1_2025)',

                           start_date DATE,
                           end_date DATE,

    -- Trạng thái: upcoming (sắp tới), active (đang diễn ra), finished (đã kết thúc)
                           status VARCHAR(20) DEFAULT 'upcoming',

    -- Thứ tự sắp xếp (1, 2, 3...) quan trọng để biết học kỳ nào trước/sau
                           priority INTEGER DEFAULT 1,

                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                           updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Khóa ngoại đến bảng năm học
                           CONSTRAINT fk_semesters_school_year FOREIGN KEY (school_year_id) REFERENCES school_years(id) ON DELETE CASCADE,

    -- Đảm bảo trong 1 năm học không có 2 học kỳ trùng tên hoặc trùng thứ tự
                           CONSTRAINT uq_semester_name_in_year UNIQUE (school_year_id, name),
                           CONSTRAINT uq_semester_priority_in_year UNIQUE (school_year_id, priority)
);

-- Cập nhật bảng teaching_assignments
ALTER TABLE teaching_assignments
    ADD CONSTRAINT fk_teaching_assignments_semester
        FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE CASCADE;
