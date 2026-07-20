-- ==============================================================================
-- Migration Script: Cập nhật Database theo Thiết kế Mới (English language center.sql)
-- Bao gồm: Gỡ bỏ Invoice/Doanh thu ngày, cập nhật Bài tập/Bài nộp,
-- thêm Phân hệ Ngân hàng câu hỏi & AI Agent + RAG
-- ==============================================================================

-- 1. GỠ BỎ PHÂN HỆ TÀI CHÍNH & BÁO CÁO CŨ
DROP TABLE IF EXISTS invoices;
DROP TABLE IF EXISTS report_daily_revenue;

-- 2. CẬP NHẬT BẢNG teaching_substitutions
ALTER TABLE teaching_substitutions DROP FOREIGN KEY fk_sub_approver;
ALTER TABLE teaching_substitutions DROP COLUMN approved_by;
ALTER TABLE teaching_substitutions MODIFY COLUMN status VARCHAR(20) DEFAULT 'SCHEDULED';

-- 3. CẬP NHẬT PHÂN HỆ LMS (assignments & submissions)
ALTER TABLE assignments DROP COLUMN file_url;
ALTER TABLE assignments ADD COLUMN time_limit_minutes INT DEFAULT 0;
ALTER TABLE assignments ADD COLUMN max_attempts INT DEFAULT 1;

ALTER TABLE submissions DROP COLUMN file_url;
ALTER TABLE submissions DROP COLUMN content;
ALTER TABLE submissions ADD COLUMN start_time DATETIME;
ALTER TABLE submissions ADD COLUMN end_time DATETIME;

-- 4. THÊM MỚI BẢNG CHO NGÂN HÀNG CÂU HỎI (QUESTION BANK)
CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    media_url VARCHAR(1000),
    reading_passage TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_content TEXT NOT NULL,
    is_correct TINYINT(1) DEFAULT 0,
    CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS assignment_questions (
    assignment_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    order_number INT NOT NULL,
    score_weight DECIMAL(5,2) DEFAULT 1.00,
    PRIMARY KEY (assignment_id, question_id),
    CONSTRAINT fk_aq_assign FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_aq_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS submission_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_id BIGINT,
    text_answer TEXT,
    earned_score DECIMAL(5,2) DEFAULT 0.00,
    is_auto_graded TINYINT(1) DEFAULT 0,
    CONSTRAINT fk_sa_submission FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_sa_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT fk_sa_option FOREIGN KEY (selected_option_id) REFERENCES question_options(id) ON DELETE SET NULL
);

-- 5. THÊM MỚI BẢNG THỐNG KÊ TRUNG TÂM (REPORTING)
CREATE TABLE IF NOT EXISTS report_center_statistics (
    report_date DATE PRIMARY KEY,
    total_active_students INT DEFAULT 0,
    new_students_today INT DEFAULT 0,
    dropped_students_today INT DEFAULT 0,
    total_teachers INT DEFAULT 0,
    new_teachers_today INT DEFAULT 0,
    resigned_teachers_today INT DEFAULT 0,
    total_other_staffs INT DEFAULT 0,
    new_staffs_today INT DEFAULT 0,
    resigned_staffs_today INT DEFAULT 0,
    total_courses INT DEFAULT 0,
    total_active_classes INT DEFAULT 0,
    new_classes_opened INT DEFAULT 0,
    classes_closed_today INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. THÊM MỚI PHÂN HỆ AI AGENT & RAG
CREATE TABLE IF NOT EXISTS ai_chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    class_id BIGINT,
    title NVARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_aichat_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_aichat_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS ai_chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aimsg_session FOREIGN KEY (session_id) REFERENCES ai_chat_sessions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ai_knowledge_bases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    material_id BIGINT,
    title NVARCHAR(255) NOT NULL,
    processing_status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aidoc_kb FOREIGN KEY (kb_id) REFERENCES ai_knowledge_bases(id) ON DELETE CASCADE,
    CONSTRAINT fk_aidoc_material FOREIGN KEY (material_id) REFERENCES learning_materials(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS ai_document_chunks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(768),
    CONSTRAINT fk_aichunk_doc FOREIGN KEY (document_id) REFERENCES ai_documents(id) ON DELETE CASCADE
);

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
