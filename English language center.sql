    -- ==============================================================================
-- Migration Script: Khởi tạo Database cho Hệ thống Quản lý Trung tâm Anh ngữ
-- Bao gồm: RBAC, Nhân sự, Học viên, EMS, LMS, Tài chính, Báo cáo
-- ==============================================================================
CREATE DATABASE IF NOT EXISTS lms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lms_db;
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
    user_id BIGINT UNIQUE NULL,
    department_id BIGINT,
    staff_code VARCHAR(50) UNIQUE NOT NULL,
    staff_type VARCHAR(50) NOT NULL, -- TEACHER, TEACHING_ASSISTANT, CONSULTANT, MANAGER
    full_name NVARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    date_of_birth DATE,
    gender NVARCHAR(20),
    address NVARCHAR(500),
    nationality VARCHAR(50) DEFAULT 'Vietnam',
    identity_number VARCHAR(50) UNIQUE NOT NULL,
    hire_date DATE,
    contract_type VARCHAR(50), -- FULLTIME, PARTTIME, VISITING
    base_salary DECIMAL(15, 2) DEFAULT 0.00,
    status VARCHAR(50) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_dept FOREIGN KEY (department_id) REFERENCES departments(id)
);

CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE NULL,
    student_code VARCHAR(50) UNIQUE NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    date_of_birth DATE,
    gender NVARCHAR(20),
    address NVARCHAR(500),
    phone VARCHAR(20) NULL,
    identity_number VARCHAR(50) UNIQUE NULL,
    parent_name NVARCHAR(100),
    parent_phone VARCHAR(20),
    target_score VARCHAR(50), -- VD: IELTS 7.0
    status VARCHAR(50) DEFAULT 'STUDYING' NOT NULL,
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
    total_sessions INT DEFAULT 0,
    base_price DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
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

CREATE TABLE IF NOT EXISTS teaching_substitutions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    absent_staff_id BIGINT NOT NULL,
    substitute_staff_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'SCHEDULED', -- SCHEDULED, COMPLETED, CANCELLED
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sub_schedule FOREIGN KEY (schedule_id) REFERENCES class_schedules(id),
    CONSTRAINT fk_sub_absent FOREIGN KEY (absent_staff_id) REFERENCES staffs(id),
    CONSTRAINT fk_sub_substitute FOREIGN KEY (substitute_staff_id) REFERENCES staffs(id)
);

-- ------------------------------------------------------------------------------
-- 5. PHÂN HỆ ENROLLMENT & FINANCE (Tuyển sinh)
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
    title VARCHAR(255) NOT NULL, -- Tiêu đề tài liệu (VD: Slide Bài 1, Link Tham khảo)
    material_type VARCHAR(50) NOT NULL, -- Các giá trị: DOCUMENT (PDF/Word), SLIDE, VIDEO, IMAGE, EXTERNAL_LINK
    source_type VARCHAR(50) DEFAULT 'MINIO', -- Các giá trị: MINIO, EXTERNAL
    resource_url TEXT NOT NULL, -- Đường dẫn Object trên MinIO HOẶC một đường link HTTP thông thường
    file_size BIGINT NULL, -- Dung lượng file (tính bằng byte) để hiển thị cho Học viên. Để NULL nếu là EXTERNAL_LINK
    display_order INT DEFAULT 0, -- Thứ tự sắp xếp tài liệu từ trên xuống dưới trong 1 bài học
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lm_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id BIGINT NOT NULL,
    title NVARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATETIME NOT NULL,
    assignment_type VARCHAR(50) DEFAULT 'HOMEWORK',
    time_limit_minutes INT DEFAULT 0,
    max_attempts INT DEFAULT 1,
    show_correct_answers BOOLEAN DEFAULT TRUE,
    status VARCHAR(20) DEFAULT 'PUBLISHED', -- UNPUBLISHED, PUBLISHED, CLOSED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assignment_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id)
);

CREATE TABLE IF NOT EXISTS submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    score DECIMAL(5, 2),
    feedback TEXT,
    start_time DATETIME,
    end_time DATETIME,
    status VARCHAR(20) DEFAULT 'SUBMITTED', -- LATE, SUBMITTED, GRADED
    submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_submission_assign FOREIGN KEY (assignment_id) REFERENCES assignments(id),
    CONSTRAINT fk_submission_student FOREIGN KEY (student_id) REFERENCES students(id)
);

-- Bảng lưu trữ nội dung từng câu hỏi (Ngân hàng câu hỏi)
CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_type VARCHAR(50) NOT NULL, -- MULTIPLE_CHOICE, FILL_BLANK, ESSAY, LISTENING, READING
    content TEXT NOT NULL, -- Nội dung câu hỏi (VD: "What is the capital of Vietnam?")
    media_url VARCHAR(1000), -- Link file Audio (Listening) hoặc Hình ảnh từ MinIO
    reading_passage TEXT, -- Đoạn văn dài dùng cho bài Reading comprehension
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng lưu trữ các đáp án lựa chọn (Dành cho trắc nghiệm A, B, C, D)
CREATE TABLE IF NOT EXISTS question_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_content TEXT NOT NULL, -- Nội dung đáp án (VD: "Hanoi")
    is_correct TINYINT(1) DEFAULT 0, -- 1 là đáp án đúng, 0 là sai
    CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- Bảng trung gian ráp Câu hỏi vào Bài tập (Nhiều - Nhiều)
CREATE TABLE IF NOT EXISTS assignment_questions (
    assignment_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    order_number INT NOT NULL, -- Thứ tự câu hỏi trong đề thi (Câu 1, Câu 2...)
    score_weight DECIMAL(5,2) DEFAULT 1.00, -- Điểm số của câu này (VD: 0.5 điểm)
    PRIMARY KEY (assignment_id, question_id),
    CONSTRAINT fk_aq_assign FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_aq_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- Bảng lưu chi tiết bài làm của học viên
CREATE TABLE IF NOT EXISTS submission_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_id BIGINT, -- Lưu ID của đáp án nếu là bài trắc nghiệm
    text_answer TEXT, -- Lưu đoạn văn học viên tự gõ nếu là bài Tự luận (Writing) / Điền từ
    earned_score DECIMAL(5,2) DEFAULT 0.00, -- Điểm đạt được cho riêng câu này
    is_auto_graded TINYINT(1) DEFAULT 0, -- Đánh dấu câu này máy đã tự chấm hay chờ giáo viên chấm
    CONSTRAINT fk_sa_submission FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_sa_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT fk_sa_option FOREIGN KEY (selected_option_id) REFERENCES question_options(id) ON DELETE SET NULL
);

-- Bảng lưu trữ điểm danh học viên theo từng buổi học
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

CREATE TABLE IF NOT EXISTS report_center_statistics (
    report_date DATE PRIMARY KEY,
    
    -- 1. Thống kê Học viên
    total_active_students INT DEFAULT 0, 
    new_students_today INT DEFAULT 0,      
    dropped_students_today INT DEFAULT 0,  
    
    -- 2. Thống kê Nhân sự (Đã cập nhật sự đối xứng)
    total_teachers INT DEFAULT 0, 
    new_teachers_today INT DEFAULT 0,      -- Giáo viên mới gia nhập trong ngày
    resigned_teachers_today INT DEFAULT 0, -- Giáo viên nghỉ việc trong ngày
    total_other_staffs INT DEFAULT 0, 
    new_staffs_today INT DEFAULT 0,        -- Nhân viên mới gia nhập
    resigned_staffs_today INT DEFAULT 0,   -- Nhân viên nghỉ việc
    
    -- 3. Thống kê Khóa học & Lớp học
    total_courses INT DEFAULT 0, 
    total_active_classes INT DEFAULT 0, 
    new_classes_opened INT DEFAULT 0,      
    classes_closed_today INT DEFAULT 0,    
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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



-- ------------------------------------------------------------------------------
-- 9. PHÂN HỆ AI AGENT & RAG (Trợ lý Ảo & Cá nhân hóa Học tập)
-- ------------------------------------------------------------------------------

-- Bảng quản lý các phiên trò chuyện của học viên với AI
CREATE TABLE IF NOT EXISTS ai_chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    class_id BIGINT, 
    title NVARCHAR(255), 
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, ARCHIVED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_aichat_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_aichat_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE SET NULL
);

-- Bảng lưu trữ chi tiết từng tin nhắn trong phiên
CREATE TABLE IF NOT EXISTS ai_chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL, -- USER, ASSISTANT, SYSTEM
    content TEXT NOT NULL,
    prompt_tokens INT DEFAULT 0, 
    completion_tokens INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aimsg_session FOREIGN KEY (session_id) REFERENCES ai_chat_sessions(id) ON DELETE CASCADE
);

-- Bảng quản lý các tập tài liệu đưa cho AI học (Knowledge Base)
CREATE TABLE IF NOT EXISTS ai_knowledge_bases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name NVARCHAR(255) NOT NULL, 
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng quản lý tài liệu vật lý được nạp vào RAG
-- Link trực tiếp tới bảng learning_materials để đồng bộ file học liệu
CREATE TABLE IF NOT EXISTS ai_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    material_id BIGINT, 
    title NVARCHAR(255) NOT NULL,
    processing_status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, EMBEDDING, SUCCESS, FAILED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aidoc_kb FOREIGN KEY (kb_id) REFERENCES ai_knowledge_bases(id) ON DELETE CASCADE,
    CONSTRAINT fk_aidoc_material FOREIGN KEY (material_id) REFERENCES learning_materials(id) ON DELETE SET NULL
);

-- Bảng lưu trữ các đoạn văn bản nhỏ (Chunks) và Vector
CREATE TABLE IF NOT EXISTS ai_document_chunks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL, 
    embedding VECTOR(768), -- Lưu trữ Vector dưới dạng Array (VD: [0.12, -0.05, ...])
    CONSTRAINT fk_aichunk_doc FOREIGN KEY (document_id) REFERENCES ai_documents(id) ON DELETE CASCADE
);

-- Bảng lưu trữ Hồ sơ cá nhân hóa (Agentic Profiling) do AI tự phân tích
CREATE TABLE IF NOT EXISTS ai_student_insights (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    class_id BIGINT, 
    weaknesses TEXT, 
    strengths TEXT, 
    recommended_path TEXT, 
    last_analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, 
    CONSTRAINT fk_aiinsight_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_aiinsight_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    UNIQUE KEY uk_insight_student_class (student_id, class_id)
);