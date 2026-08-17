-- V9__Update_staff_student_course_room.sql

-- 1. Bổ sung các trường vào bảng staffs
ALTER TABLE staffs 
    ADD COLUMN date_of_birth DATE,
    ADD COLUMN gender NVARCHAR(20),
    ADD COLUMN address NVARCHAR(500),
    ADD COLUMN nationality VARCHAR(50) DEFAULT 'Vietnam',
    ADD COLUMN identity_number VARCHAR(50);
    
-- Thêm UNIQUE constraint riêng biệt để tránh lỗi nếu dữ liệu hiện tại có trùng
ALTER TABLE staffs ADD CONSTRAINT uk_staffs_identity_number UNIQUE (identity_number);

-- 2. Bổ sung các trường vào bảng students
ALTER TABLE students 
    ADD COLUMN date_of_birth DATE,
    ADD COLUMN gender NVARCHAR(20),
    ADD COLUMN address NVARCHAR(500),
    ADD COLUMN phone VARCHAR(20) NULL,
    ADD COLUMN identity_number VARCHAR(50) NULL;
    
ALTER TABLE students ADD CONSTRAINT uk_students_identity_number UNIQUE (identity_number);

-- 3. Bổ sung trường vào bảng courses
ALTER TABLE courses 
    ADD COLUMN total_sessions INT DEFAULT 0;

-- 4. Bỏ trường room_type khỏi bảng rooms
ALTER TABLE rooms
    DROP COLUMN room_type;
