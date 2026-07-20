-- ==============================================================================
-- Migration Script: Khởi tạo Database cho Hệ thống Quản lý Trung tâm Anh ngữ
-- Bao gồm: RBAC, Nhân sự, Học viên, EMS, LMS, Tài chính, Báo cáo
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. PHÂN HỆ CORE & RBAC (Quản lý Quyền và Người dùng cốt lõi)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     email VARCHAR(100) UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name NVARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, LOCKED
    refresh_token VARCHAR(500),
    expiry_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS roles (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     name NVARCHAR(50) UNIQUE NOT NULL, -- ROLE_ADMIN, ROLE_MANAGER, ROLE_TEACHER, ROLE_STUDENT
    description NVARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS permissions (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           name NVARCHAR(100) UNIQUE NOT NULL, -- COURSE_CREATE, CLASS_VIEW, INVOICE_MANAGE
    description NVARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT NOT NULL,
                                          role_id BIGINT NOT NULL,
                                          PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS role_permissions (
                                                role_id BIGINT NOT NULL,
                                                permission_id BIGINT NOT NULL,
                                                PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS departments (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           code VARCHAR(50) UNIQUE NOT NULL,
    name NVARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- ------------------------------------------------------------------------------
-- 2. PHÂN HỆ NHÂN SỰ & HỌC VIÊN (Hồ sơ chi tiết)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS staffs (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      user_id BIGINT UNIQUE NOT NULL,
                                      department_id BIGINT,
                                      staff_code VARCHAR(50) UNIQUE NOT NULL,
    staff_type VARCHAR(50) NOT NULL, -- TEACHER, TEACHING_ASSISTANT, CONSULTANT, MANAGER
    full_name NVARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    hire_date DATE,
    contract_type VARCHAR(50), -- FULLTIME, PARTTIME, VISITING
    base_salary DECIMAL(15, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_dept FOREIGN KEY (department_id) REFERENCES departments(id)
    );

CREATE TABLE IF NOT EXISTS students (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        user_id BIGINT UNIQUE NOT NULL,
                                        student_code VARCHAR(50) UNIQUE NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    parent_name NVARCHAR(100),
    parent_phone VARCHAR(20),
    target_score VARCHAR(50), -- VD: IELTS 7.0
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- ------------------------------------------------------------------------------
-- 3. PHÂN HỆ ACADEMIC (Quản lý Khóa học, Lớp học, Lịch học)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS courses (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       code VARCHAR(50) UNIQUE NOT NULL,
    name NVARCHAR(255) NOT NULL,
    description TEXT,
    duration_hours INT DEFAULT 0,
    base_price DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS rooms (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     name NVARCHAR(100) NOT NULL,
    room_type VARCHAR(20) DEFAULT 'PHYSICAL', -- PHYSICAL, ONLINE
    capacity INT DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Bảng Terms (Kỳ / Đợt vận hành)
CREATE TABLE IF NOT EXISTS terms (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     code VARCHAR(50) UNIQUE NOT NULL, -- VD: SUMMER-2026, Q1-2026
    name NVARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    year INT NOT NULL, -- Phục vụ gom nhóm nhanh theo năm (VD: 2026)
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, CLOSED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS classes (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       course_id BIGINT NOT NULL,
                                       term_id BIGINT,
                                       code VARCHAR(50) UNIQUE NOT NULL,
    name NVARCHAR(255) NOT NULL,
    start_date DATE,
    end_date DATE,
    max_students INT DEFAULT 20,
    current_students INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'OPENING', -- OPENING, ONGOING, CLOSED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_course FOREIGN KEY (course_id) REFERENCES courses(id),
    CONSTRAINT fk_class_term FOREIGN KEY (term_id) REFERENCES terms(id)
    );

CREATE TABLE IF NOT EXISTS class_schedules (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               class_id BIGINT NOT NULL,
                                               room_id BIGINT,
                                               day_of_week INT NOT NULL, -- 2: Thứ 2 ... 8: Chủ nhật
                                               start_time TIME NOT NULL,
                                               end_time TIME NOT NULL,
                                               CONSTRAINT fk_schedule_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_schedule_room FOREIGN KEY (room_id) REFERENCES rooms(id)
    );

-- ------------------------------------------------------------------------------
-- 4. PHÂN HỆ TEACHING ASSIGNMENT (Phân công giảng dạy & Dạy thay)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS teaching_assignments (
                                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                    class_id BIGINT NOT NULL,
                                                    staff_id BIGINT NOT NULL,
                                                    role VARCHAR(50) DEFAULT 'MAIN_TEACHER', -- MAIN_TEACHER, NATIVE_TEACHER, ASSISTANT
    assigned_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, TRANSFERRED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assign_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_assign_staff FOREIGN KEY (staff_id) REFERENCES staffs(id)
    );

CREATE TABLE IF NOT EXISTS teaching_substitutions (
                                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                      schedule_id BIGINT NOT NULL,
                                                      absent_staff_id BIGINT NOT NULL,
                                                      substitute_staff_id BIGINT NOT NULL,
                                                      reason TEXT NOT NULL,
                                                      status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    approved_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sub_schedule FOREIGN KEY (schedule_id) REFERENCES class_schedules(id),
    CONSTRAINT fk_sub_absent FOREIGN KEY (absent_staff_id) REFERENCES staffs(id),
    CONSTRAINT fk_sub_substitute FOREIGN KEY (substitute_staff_id) REFERENCES staffs(id),
    CONSTRAINT fk_sub_approver FOREIGN KEY (approved_by) REFERENCES staffs(id)
    );

-- ------------------------------------------------------------------------------
-- 5. PHÂN HỆ ENROLLMENT & FINANCE (Tuyển sinh & Hóa đơn)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS enrollments (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           student_id BIGINT NOT NULL,
                                           class_id BIGINT NOT NULL,
                                           enrollment_date DATE NOT NULL,
                                           status VARCHAR(20) DEFAULT 'ACTIVE', -- PENDING, ACTIVE, DROPPED, COMPLETED
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_enroll_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_enroll_class FOREIGN KEY (class_id) REFERENCES classes(id),
    UNIQUE KEY uk_student_class (student_id, class_id)
    );

CREATE TABLE IF NOT EXISTS invoices (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        invoice_code VARCHAR(50) UNIQUE NOT NULL,
    enrollment_id BIGINT NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    discount_amount DECIMAL(15, 2) DEFAULT 0.00,
    paid_amount DECIMAL(15, 2) DEFAULT 0.00,
    payment_status VARCHAR(20) DEFAULT 'UNPAID', -- UNPAID, PARTIAL, PAID
    payment_method VARCHAR(50),
    due_date DATE,
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_enroll FOREIGN KEY (enrollment_id) REFERENCES enrollments(id)
    );

-- ------------------------------------------------------------------------------
-- 6. PHÂN HỆ LMS (Học liệu, Bài tập, Điểm danh, Thông báo)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS lessons (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       class_id BIGINT NOT NULL,
                                       name NVARCHAR(255) NOT NULL,
    order_number INT NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lesson_class FOREIGN KEY (class_id) REFERENCES classes(id)
    );

CREATE TABLE IF NOT EXISTS learning_materials (
                                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                  lesson_id BIGINT NOT NULL,
                                                  title NVARCHAR(255) NOT NULL,
    file_url VARCHAR(1000) NOT NULL,
    material_type VARCHAR(20), -- VIDEO, PDF, AUDIO, SLIDE
    is_preview BOOLEAN DEFAULT FALSE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_material_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id)
    );

CREATE TABLE IF NOT EXISTS assignments (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           lesson_id BIGINT NOT NULL,
                                           title NVARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATETIME NOT NULL,
    assignment_type VARCHAR(50) DEFAULT 'HOMEWORK',
    file_url VARCHAR(1000),
    status VARCHAR(20) DEFAULT 'PUBLISHED', -- UNPUBLISHED, PUBLISHED, CLOSED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assignment_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id)
    );

CREATE TABLE IF NOT EXISTS submissions (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           assignment_id BIGINT NOT NULL,
                                           student_id BIGINT NOT NULL,
                                           file_url VARCHAR(1000),
    content TEXT,
    score DECIMAL(5, 2),
    feedback TEXT,
    status VARCHAR(20) DEFAULT 'SUBMITTED', -- LATE, SUBMITTED, GRADED
    submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_submission_assign FOREIGN KEY (assignment_id) REFERENCES assignments(id),
    CONSTRAINT fk_submission_student FOREIGN KEY (student_id) REFERENCES students(id)
    );

CREATE TABLE IF NOT EXISTS attendance (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          schedule_id BIGINT NOT NULL,
                                          student_id BIGINT NOT NULL,
                                          attendance_date DATE NOT NULL,
                                          status VARCHAR(20) DEFAULT 'PRESENT', -- PRESENT, ABSENT, EXCUSED, LATE
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attend_schedule FOREIGN KEY (schedule_id) REFERENCES class_schedules(id),
    CONSTRAINT fk_attend_student FOREIGN KEY (student_id) REFERENCES students(id),
    UNIQUE KEY uk_attend_date (schedule_id, student_id, attendance_date)
    );

CREATE TABLE IF NOT EXISTS class_announcements (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   class_id BIGINT NOT NULL,
                                                   created_by BIGINT NOT NULL, -- Có thể là Giáo viên hoặc Quản lý
                                                   title NVARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    has_attachment BOOLEAN DEFAULT FALSE,
    attachment_url VARCHAR(1000),
    is_pinned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_announce_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_announce_user FOREIGN KEY (created_by) REFERENCES users(id)
    );

-- ------------------------------------------------------------------------------
-- 7. PHÂN HỆ REPORTING (Báo cáo Snapshot)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS report_daily_revenue (
                                                    report_date DATE PRIMARY KEY,
                                                    term_id BIGINT,
                                                    total_enrollments INT DEFAULT 0,
                                                    total_revenue_expected DECIMAL(15, 2) DEFAULT 0.00,
    total_revenue_collected DECIMAL(15, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS report_class_metrics (
                                                    class_id BIGINT PRIMARY KEY,
                                                    total_students INT DEFAULT 0,
                                                    average_attendance_rate DECIMAL(5, 2) DEFAULT 0.00,
    average_assignment_score DECIMAL(5, 2) DEFAULT 0.00,
    dropped_students INT DEFAULT 0,
    last_calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_metric_class FOREIGN KEY (class_id) REFERENCES classes(id)
    );

-- ============================================
-- 8. BẢNG BỔ TRỢ
-- ============================================

CREATE TABLE activity_logs (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id BIGINT,
                               actor_name NVARCHAR(100),
                               module VARCHAR(50), -- Phân hệ (Để lọc nhanh: Chỉ xem log điểm, Log đăng nhập...)
                               action VARCHAR(50) NOT NULL, -- Hành động cụ thể (VD: 'LOGIN', 'UPDATE_SCORE', 'DELETE_CLASS')
                               target_type VARCHAR(50), -- VD: 'assignment_submissions'
                               target_id VARCHAR(36),   -- ID của dòng bị sửa
                               details JSON, -- Chứa: dữ liệu cũ, dữ liệu mới, lý do...
    -- Trạng thái hành động (Quan trọng để phát hiện hack/lỗi)
                               status VARCHAR(20) DEFAULT 'success' CHECK (status IN ('success', 'failure', 'error')),
                               ip_address VARCHAR(45),
                               user_agent TEXT, -- Lưu tên trình duyệt/thiết bị
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_activity_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);