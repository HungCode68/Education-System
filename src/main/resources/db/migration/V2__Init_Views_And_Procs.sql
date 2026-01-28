-- ============================================
-- VIEWS CHO THỐNG KÊ TIẾN ĐỘ BÀI TẬP
-- ============================================
-- VIEW 1: Thống kê tổng quan bài tập
-- Mục đích: Xem danh sách các bài tập và tổng số học sinh đã nộp bài
-- ============================================

CREATE OR REPLACE VIEW vw_assignment_statistics AS
SELECT
    a.id AS assignment_id,
    a.title AS assignment_title,
    a.description AS assignment_description,
    a.assignment_type,
    a.start_time,
    a.due_time,
    a.max_score,
    a.status AS assignment_status,
    a.created_at AS assignment_created_at,

    -- Thông tin lớp học & Giáo viên (Lấy từ bảng teaching_assignments)
    oc.id AS online_class_id,
    oc.name AS online_class_name,

    pc.id AS physical_class_id,
    pc.name AS physical_class_name,

    s.id AS subject_id,
    s.name AS subject_name,

    t.id AS teacher_id,
    t.full_name AS teacher_name,

    sy.id AS school_year_id,
    sy.name AS school_year_name,

    -- 1. Tổng số học sinh trong lớp (Mẫu số)
    (SELECT COUNT(*)
     FROM online_class_students ocs
     WHERE ocs.online_class_id = oc.id
       AND ocs.status = 'active') AS total_students,

    -- 2. Số học sinh ĐÃ nộp bài (submitted + late + graded)
    (SELECT COUNT(*)
     FROM assignment_submissions asub
     WHERE asub.assignment_id = a.id
       AND asub.submission_status IN ('submitted', 'late', 'graded')) AS submitted_count,

    -- 3. Số bài nộp đúng hạn
    (SELECT COUNT(*)
     FROM assignment_submissions asub
     WHERE asub.assignment_id = a.id
       AND asub.submission_status IN ('submitted', 'graded')
       AND asub.is_late = FALSE) AS on_time_count,

    -- 4. Số bài nộp trễ
    (SELECT COUNT(*)
     FROM assignment_submissions asub
     WHERE asub.assignment_id = a.id
       AND (asub.is_late = TRUE OR asub.submission_status = 'late')) AS late_count,

    -- 5. Số bài ĐÃ CHẤM
    (SELECT COUNT(*)
     FROM assignment_submissions asub
     WHERE asub.assignment_id = a.id
       AND asub.submission_status = 'graded') AS graded_count,

    -- 7. Tỷ lệ hoàn thành (%)
    ROUND(
            IFNULL(
                    (SELECT COUNT(*)
                     FROM assignment_submissions asub
                     WHERE asub.assignment_id = a.id
                       AND asub.submission_status IN ('submitted', 'late', 'graded')) * 100.0 /
                    NULLIF((SELECT COUNT(*)
                            FROM online_class_students ocs
                            WHERE ocs.online_class_id = oc.id
                              AND ocs.status = 'active'), 0)
                , 0),
            2
    ) AS completion_rate,

    -- 8. Số học sinh CHƯA nộp (Lấy Tổng - Đã nộp)
    (
        (SELECT COUNT(*) FROM online_class_students ocs WHERE ocs.online_class_id = oc.id AND ocs.status = 'active')
            -
        (SELECT COUNT(*) FROM assignment_submissions asub WHERE asub.assignment_id = a.id AND asub.submission_status IN ('submitted', 'late', 'graded'))
        ) AS not_submitted_count

FROM assignments a
         JOIN online_classes oc ON a.online_class_id = oc.id
         JOIN teaching_assignments ta ON oc.teaching_assignment_id = ta.id
         JOIN physical_classes pc ON ta.physical_class_id = pc.id
         JOIN subjects s ON ta.subject_id = s.id
         JOIN teachers t ON ta.teacher_id = t.id
         JOIN school_years sy ON ta.school_year_id = sy.id;


-- ============================================
-- VIEW 2: Chi tiết danh sách học sinh theo bài tập
-- Mục đích: Xem chi tiết danh sách học sinh với trạng thái làm bài
-- ============================================

CREATE OR REPLACE VIEW vw_assignment_student_details AS
SELECT
    -- 1. Thông tin Bài tập
    a.id AS assignment_id,
    a.title AS assignment_title,
    a.due_time,
    a.max_score,

    -- 2. Thông tin Lớp học
    oc.id AS online_class_id,
    oc.name AS online_class_name,
    pc.id AS physical_class_id,
    pc.name AS physical_class_name,

    -- 3. Thông tin Học sinh
    st.id AS student_id,
    st.student_code, -- Thêm mã HS để giáo viên dễ tìm
    st.full_name AS student_name,
    u.email AS student_email,

    -- 4. Thông tin Nộp bài (Lấy từ bảng submissions)
    asub.id AS submission_id,

    -- Nếu chưa có dòng record nào thì coi như 'not_submitted'
    COALESCE(asub.submission_status, 'not_submitted') AS submission_status,

    asub.submitted_at,
    asub.is_late,
    asub.attempt_count, -- Thêm số lần nộp để biết HS có nộp lại không

    -- 5. Thông tin Điểm số (Đã gộp từ bảng grades cũ sang)
    asub.score,
    asub.teacher_feedback,
    asub.graded_at,
    asub.graded_by,
    asub.grading_method, -- 'auto' hoặc 'manual'

    -- 6. Trạng thái hiển thị (Logic ưu tiên)
    CASE
        WHEN asub.submission_status IS NULL OR asub.submission_status = 'not_submitted' THEN 'Chưa nộp'
        WHEN asub.submission_status = 'draft' THEN 'Lưu nháp'
        WHEN asub.submission_status = 'graded' THEN CONCAT('Đã chấm (', asub.score, 'đ)')
        WHEN asub.submission_status = 'submitted' AND asub.is_late = TRUE THEN 'Nộp muộn'
        WHEN asub.submission_status = 'submitted' THEN 'Đã nộp'
        ELSE 'Khác'
        END AS display_status_text,

    -- 7. Tính thời gian trễ hoặc còn lại (Đơn vị: Phút)
    -- Số dương: Còn lại X phút. Số âm: Đã trễ X phút.
    TIMESTAMPDIFF(MINUTE, NOW(), a.due_time) AS minutes_remaining

FROM assignments a
-- [QUAN TRỌNG] Fix đường dẫn JOIN chuẩn
         INNER JOIN online_classes oc ON a.online_class_id = oc.id
         INNER JOIN teaching_assignments ta ON oc.teaching_assignment_id = ta.id
         INNER JOIN physical_classes pc ON ta.physical_class_id = pc.id

-- Lấy danh sách TẤT CẢ học sinh đang học trong lớp đó (Active)
         INNER JOIN online_class_students ocs ON oc.id = ocs.online_class_id AND ocs.status = 'active'
         INNER JOIN students st ON ocs.student_id = st.id
         INNER JOIN users u ON st.user_id = u.id

-- LEFT JOIN để vẫn hiện ra học sinh kể cả khi họ chưa nộp bài
         LEFT JOIN assignment_submissions asub ON a.id = asub.assignment_id AND st.id = asub.student_id;


-- ============================================
-- VIEW 3: Thống kê theo lớp online
-- Mục đích: Tổng quan tiến độ tất cả bài tập trong một lớp
-- ============================================

CREATE OR REPLACE VIEW vw_class_assignment_summary AS
SELECT
    oc.id AS online_class_id,
    oc.name AS online_class_name,

    t.id AS teacher_id,
    sy.id AS school_year_id,

    pc.id AS physical_class_id,
    pc.name AS physical_class_name,

    s.name AS subject_name,
    t.full_name AS teacher_name,

    --  Thêm Học kỳ và Năm học (Rất cần để lọc báo cáo)
    ta.semester_id,
    sy.name AS school_year_name,

    -- 1. Các chỉ số số lượng bài tập
    COUNT(DISTINCT a.id) AS total_assignments,
    COUNT(DISTINCT CASE WHEN a.status = 'published' THEN a.id END) AS published_assignments,
    COUNT(DISTINCT CASE WHEN a.status = 'unpublished' THEN a.id END) AS unpublished_assignments,

    -- 2. Số bài tập đã quá hạn (Deadline đã qua)
    COUNT(DISTINCT CASE WHEN a.due_time < NOW() AND a.status = 'published' THEN a.id END) AS overdue_assignments,

    -- 3. Tổng số bài nộp ĐANG CHỜ CHẤM (Call to Action cho giáo viên)
    -- Giúp giáo viên biết lớp nào đang tồn đọng nhiều bài chưa chấm nhất
    (SELECT COUNT(*)
     FROM assignment_submissions asub
              JOIN assignments a2 ON asub.assignment_id = a2.id
     WHERE a2.online_class_id = oc.id
       AND asub.submission_status = 'submitted') AS submissions_needing_grading,

    -- 4.Điểm trung bình của toàn bộ lớp (Dựa trên các bài đã chấm)
    (SELECT ROUND(AVG(asub.score), 2)
     FROM assignment_submissions asub
              JOIN assignments a3 ON asub.assignment_id = a3.id
     WHERE a3.online_class_id = oc.id
       AND asub.score IS NOT NULL) AS avg_class_score,

    -- 5. Tỷ lệ hoàn thành trung bình
    ROUND(
            AVG(
                    IFNULL(
                            (SELECT COUNT(*)
                             FROM assignment_submissions asub
                             WHERE asub.assignment_id = a.id
                               AND asub.submission_status IN ('submitted', 'late', 'graded')) * 100.0 /
                            NULLIF((SELECT COUNT(*)
                                    FROM online_class_students ocs2
                                    WHERE ocs2.online_class_id = oc.id
                                      AND ocs2.status = 'active'), 0)
                        , 0)
            ),
            2) AS avg_completion_rate

FROM online_classes oc
         INNER JOIN teaching_assignments ta ON oc.teaching_assignment_id = ta.id
         INNER JOIN physical_classes pc ON ta.physical_class_id = pc.id
         INNER JOIN subjects s ON ta.subject_id = s.id
         INNER JOIN teachers t ON ta.teacher_id = t.id
         INNER JOIN school_years sy ON ta.school_year_id = sy.id
         LEFT JOIN assignments a ON oc.id = a.online_class_id
GROUP BY
    oc.id, oc.name,
    pc.id, pc.name,
    s.name, t.full_name,
    ta.semester_id, sy.name,
    t.id, sy.id;


-- ============================================
-- VIEW 4: Thống kê chi tiết theo học sinh
-- Mục đích: Xem tất cả bài tập và trạng thái của một học sinh
-- ============================================

CREATE OR REPLACE VIEW vw_student_assignment_progress AS
SELECT
    -- 1. Thông tin Học sinh
    st.id AS student_id,
    st.student_code,
    st.full_name AS student_name,
    u.email AS student_email,

    -- 2. Thông tin Lớp & Môn học
    oc.id AS online_class_id,
    oc.name AS online_class_name,

    s.id AS subject_id,
    s.name AS subject_name,

    -- Thêm ngữ cảnh thời gian (Rất quan trọng để lọc kết quả)
    sy.name AS school_year_name,
    ta.semester_id,

    -- 3. Các chỉ số thống kê
    COUNT(DISTINCT a.id) AS total_assignments,

    -- Số bài đã nộp (Bao gồm cả nộp trễ và đã chấm)
    COUNT(DISTINCT CASE WHEN asub.submission_status IN ('submitted', 'late', 'graded') THEN a.id END) AS submitted_assignments,

    -- Số bài chưa nộp (Chưa có record submission hoặc status là not_submitted/draft)
    COUNT(DISTINCT CASE
                       WHEN asub.submission_status IS NULL
                           OR asub.submission_status IN ('not_submitted', 'draft')
                           THEN a.id
        END) AS not_submitted_assignments,

    -- Số bài đã được chấm điểm
    COUNT(DISTINCT CASE WHEN asub.submission_status = 'graded' THEN a.id END) AS graded_assignments,

    -- Số bài nộp trễ
    COUNT(DISTINCT CASE WHEN asub.is_late = TRUE THEN a.id END) AS late_assignments,

    --  Điểm trung bình (Lấy trực tiếp từ bảng submissions)
    -- Chỉ tính trên các bài ĐÃ CÓ ĐIỂM
    ROUND(AVG(asub.score), 2) AS average_score,

    -- Tỷ lệ hoàn thành (%)
    ROUND(
            COUNT(DISTINCT CASE WHEN asub.submission_status IN ('submitted', 'late', 'graded') THEN a.id END) * 100.0 /
            NULLIF(COUNT(DISTINCT a.id), 0),
            2
    ) AS completion_rate

FROM students st
         INNER JOIN users u ON st.user_id = u.id

-- Lấy danh sách lớp học sinh đang tham gia
         INNER JOIN online_class_students ocs ON st.id = ocs.student_id AND ocs.status = 'active'
         INNER JOIN online_classes oc ON ocs.online_class_id = oc.id

-- Join sang bảng Assignment để lấy Môn học & Năm học
         INNER JOIN teaching_assignments ta ON oc.teaching_assignment_id = ta.id
         INNER JOIN subjects s ON ta.subject_id = s.id
         INNER JOIN school_years sy ON ta.school_year_id = sy.id

-- LEFT JOIN lấy bài tập (Chỉ lấy bài đã Published)
         LEFT JOIN assignments a ON oc.id = a.online_class_id AND a.status = 'published'

-- LEFT JOIN lấy bài làm của học sinh đó
         LEFT JOIN assignment_submissions asub ON a.id = asub.assignment_id AND st.id = asub.student_id

GROUP BY
    st.id, st.student_code, st.full_name, u.email,
    oc.id, oc.name,
    s.id, s.name,
    sy.name, ta.semester_id;


-- ============================================
-- STORED PROCEDURES CHO CÁC CHỨC NĂNG LỌC
-- ============================================
-- PROCEDURE 1: Lọc bài tập theo tên hoặc thời gian
-- ============================================

DELIMITER $$

CREATE PROCEDURE sp_filter_assignments(
    IN p_online_class_id VARCHAR(36),
    IN p_search_keyword VARCHAR(255),
    IN p_start_date DATETIME,
    IN p_end_date DATETIME,
    IN p_status VARCHAR(20),
    IN p_assignment_type VARCHAR(30)
)
BEGIN
SELECT * FROM vw_assignment_statistics
WHERE online_class_id = p_online_class_id

  -- 1. Tìm kiếm theo tên (Có xử lý case-insensitive mặc định của MySQL)
  AND (p_search_keyword IS NULL OR assignment_title LIKE CONCAT('%', p_search_keyword, '%'))

  -- 2. Lọc theo khoảng thời gian (Ngày tạo)
  AND (p_start_date IS NULL OR assignment_created_at >= p_start_date)
  AND (p_end_date IS NULL OR assignment_created_at <= p_end_date)

  -- 3. Lọc theo trạng thái (Published/Unpublished)
  AND (p_status IS NULL OR assignment_status = p_status)

  -- 4. Lọc theo loại bài tập (Multiple choice/Essay...)
  AND (p_assignment_type IS NULL OR assignment_type = p_assignment_type)

ORDER BY assignment_created_at DESC;
END$$

DELIMITER ;


-- ============================================
-- PROCEDURE 2: Lọc học sinh theo trạng thái nộp bài
-- ============================================

DELIMITER $$

CREATE PROCEDURE sp_filter_students_by_submission_status(
    IN p_assignment_id VARCHAR(36),
    IN p_submission_status VARCHAR(20), -- Input: 'not_submitted', 'submitted', 'late', 'graded'
    IN p_search_keyword VARCHAR(255)
)
BEGIN
SELECT * FROM vw_assignment_student_details
WHERE assignment_id = p_assignment_id

  -- 1. Tìm kiếm thông minh: Tên OR Mã học sinh OR Email
  AND (p_search_keyword IS NULL
    OR student_name LIKE CONCAT('%', p_search_keyword, '%')
    OR student_code LIKE CONCAT('%', p_search_keyword, '%') -- [MỚI] Tìm theo mã
    OR student_email LIKE CONCAT('%', p_search_keyword, '%'))

  -- 2. Bộ lọc trạng thái (Xử lý kỹ phần Late)
  AND (
    p_submission_status IS NULL -- Nếu không truyền gì thì lấy hết
        OR
    CASE
        -- a. Lọc người chưa nộp
        WHEN p_submission_status = 'not_submitted' THEN
            (submission_status IS NULL OR submission_status = 'not_submitted' OR submission_status = 'draft')

        -- b. Lọc người nộp muộn (Quan trọng: Phải check cờ is_late)
        WHEN p_submission_status = 'late' THEN
            (is_late = TRUE)

        -- c. Lọc người nộp đúng hạn (Status là submitted/graded VÀ không late)
        WHEN p_submission_status = 'submitted' THEN
            (submission_status IN ('submitted', 'graded') AND is_late = FALSE)

        -- d. Lọc bài đã chấm
        WHEN p_submission_status = 'graded' THEN
            (submission_status = 'graded')

        ELSE FALSE
        END
    )

-- 3. Sắp xếp ưu tiên
ORDER BY
    CASE
        WHEN submission_status IS NULL OR submission_status = 'not_submitted' THEN 1 -- Chưa nộp lên đầu
        WHEN is_late = TRUE THEN 2      -- Nộp muộn thứ 2 (để nhắc nhở)
        WHEN submission_status = 'submitted' THEN 3 -- Đã nộp
        WHEN submission_status = 'graded' THEN 4    -- Đã chấm xuống cuối
        ELSE 5
        END,
    student_name ASC;
END$$

DELIMITER ;


-- ============================================
-- PROCEDURE 3: Thống kê chi tiết một bài tập
-- ============================================

DELIMITER $$

CREATE PROCEDURE sp_get_assignment_detail_statistics(
    IN p_assignment_id VARCHAR(36)
)
BEGIN
    -- ==========================================================
    -- RESULT SET 1: THÔNG TIN TỔNG QUAN (Header)
    -- Dùng để hiển thị các thẻ số liệu: Tổng số, Đã nộp, Điểm TB...
    -- ==========================================================
SELECT * FROM vw_assignment_statistics
WHERE assignment_id = p_assignment_id;

-- ==========================================================
-- RESULT SET 2: PHỔ ĐIỂM (Score Distribution)
-- Dùng để vẽ biểu đồ cột phân loại học lực
-- ==========================================================
SELECT
    -- Đếm số lượng theo các mức điểm
    COUNT(CASE WHEN score < 5 THEN 1 END) AS count_weak,       -- Yếu (< 5)
    COUNT(CASE WHEN score >= 5 AND score < 6.5 THEN 1 END) AS count_average, -- Trung bình (5 - 6.5)
    COUNT(CASE WHEN score >= 6.5 AND score < 8 THEN 1 END) AS count_good,    -- Khá (6.5 - 8)
    COUNT(CASE WHEN score >= 8 THEN 1 END) AS count_excellent, -- Giỏi (>= 8)

    -- Điểm cao nhất và thấp nhất
    MAX(score) AS highest_score,
    MIN(score) AS lowest_score
FROM assignment_submissions
WHERE assignment_id = p_assignment_id
  AND submission_status = 'graded'; -- Chỉ tính những bài đã chấm

-- ==========================================================
-- RESULT SET 3: DANH SÁCH HỌC SINH CHI TIẾT (List)
-- Dùng để hiển thị bảng danh sách phía dưới
-- ==========================================================
SELECT * FROM vw_assignment_student_details
WHERE assignment_id = p_assignment_id
ORDER BY
    -- Sắp xếp ưu tiên: Chưa nộp -> Nháp -> Nộp muộn -> Đã nộp -> Đã chấm
    CASE
        WHEN submission_status IS NULL OR submission_status = 'not_submitted' THEN 1
        WHEN submission_status = 'draft' THEN 2
        WHEN is_late = TRUE THEN 3
        WHEN submission_status = 'submitted' THEN 4
        WHEN submission_status = 'graded' THEN 5
        ELSE 6
        END ASC,
    student_name ASC;
END$$

DELIMITER ;


-- ============================================
-- PROCEDURE 4: Dashboard thống kê cho giáo viên
-- ============================================

DELIMITER $$

CREATE PROCEDURE sp_teacher_dashboard_statistics(
    IN p_teacher_id VARCHAR(36),
    IN p_school_year_id VARCHAR(36)
)
BEGIN
    -- =================================================================
    -- 1. TỔNG QUAN (Header Dashboard)
    -- Hiển thị 4 số to đùng trên cùng: Tổng lớp, Tổng HS, Bài đã giao, Cần chấm
    -- =================================================================
SELECT
    COUNT(DISTINCT ta.id) AS total_classes,
    (SELECT COUNT(*) FROM online_class_students ocs
                              JOIN online_classes oc ON ocs.online_class_id = oc.id
                              JOIN teaching_assignments ta2 ON oc.teaching_assignment_id = ta2.id
     WHERE ta2.teacher_id = p_teacher_id AND ta2.school_year_id = p_school_year_id
       AND ocs.status = 'active') AS total_students,

    (SELECT COUNT(*) FROM assignments a
                              JOIN online_classes oc ON a.online_class_id = oc.id
                              JOIN teaching_assignments ta3 ON oc.teaching_assignment_id = ta3.id
     WHERE ta3.teacher_id = p_teacher_id AND ta3.school_year_id = p_school_year_id
       AND a.status = 'published') AS active_assignments,

    (SELECT COUNT(*) FROM assignment_submissions asub
                              JOIN assignments a ON asub.assignment_id = a.id
                              JOIN online_classes oc ON a.online_class_id = oc.id
                              JOIN teaching_assignments ta4 ON oc.teaching_assignment_id = ta4.id
     WHERE ta4.teacher_id = p_teacher_id AND ta4.school_year_id = p_school_year_id
       AND asub.submission_status = 'submitted') AS pending_grading_count;

-- =================================================================
-- 2. DANH SÁCH LỚP HỌC (Main Table)
-- Giờ đã lọc đúng theo Năm học và Giáo viên
-- =================================================================
SELECT * FROM vw_class_assignment_summary
WHERE teacher_id = p_teacher_id
  AND school_year_id = p_school_year_id
ORDER BY online_class_name;

-- =================================================================
-- 3. CẢNH BÁO: BÀI TẬP CÓ VẤN ĐỀ (Tỷ lệ nộp < 50%)
-- Giúp giáo viên đốc thúc học sinh
-- =================================================================
SELECT
    assignment_id,
    assignment_title,
    online_class_name,
    completion_rate,
    submitted_count,
    total_students
FROM vw_assignment_statistics
WHERE teacher_id = p_teacher_id
  AND school_year_id = p_school_year_id
  AND assignment_status = 'published'
  AND completion_rate < 50 -- Chỉ hiện những bài nộp quá ít
ORDER BY completion_rate ASC
    LIMIT 5;

-- =================================================================
-- 4. NHẮC VIỆC: BÀI TẬP CẦN CHẤM GẤP (MỚI & QUAN TRỌNG)
-- Liệt kê các bài tập có nhiều bài nộp chưa chấm nhất
-- =================================================================
SELECT
    a.id AS assignment_id,
    a.title AS assignment_title,
    oc.name AS class_name,
    COUNT(asub.id) AS waiting_for_grade_count,
    DATEDIFF(NOW(), MIN(asub.submitted_at)) AS oldest_submission_days -- Bài cũ nhất đã đợi bao nhiêu ngày
FROM assignment_submissions asub
         JOIN assignments a ON asub.assignment_id = a.id
         JOIN online_classes oc ON a.online_class_id = oc.id
         JOIN teaching_assignments ta ON oc.teaching_assignment_id = ta.id
WHERE ta.teacher_id = p_teacher_id
  AND ta.school_year_id = p_school_year_id
  AND asub.submission_status = 'submitted' -- Trạng thái đã nộp nhưng chưa chấm
GROUP BY a.id, a.title, oc.name
ORDER BY waiting_for_grade_count DESC -- Bài nào tồn nhiều nhất hiện lên đầu
    LIMIT 5;

-- =================================================================
-- 5. SẮP ĐẾN HẠN (Lịch trình)
-- =================================================================
SELECT
    assignment_id,
    assignment_title,
    online_class_name,
    due_time,
    completion_rate
FROM vw_assignment_statistics
WHERE teacher_id = p_teacher_id
  AND school_year_id = p_school_year_id
  AND assignment_status = 'published'
  AND due_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 3 DAY)
ORDER BY due_time ASC;

END$$

DELIMITER ;


-- ============================================
-- TẠO INDEX BỔ SUNG CHO VIEWS
-- =======================================================
-- 1. BẢNG ASSIGNMENTS (Bài tập)
-- =======================================================

-- Khi lọc bài tập của 1 lớp và sắp xếp theo ngày tạo
CREATE INDEX idx_assignments_class_created ON assignments(online_class_id, created_at);

-- Tối ưu cho Dashboard giáo viên (Lọc bài đã published + Hạn nộp)
CREATE INDEX idx_assignments_status_due ON assignments(status, due_time);

CREATE INDEX idx_assignments_due_time ON assignments(due_time);
CREATE INDEX idx_assignments_title ON assignments(title);

-- =======================================================
-- 2. BẢNG ASSIGNMENT_SUBMISSIONS (Nộp bài)
-- =======================================================

-- Giúp đếm số lượng (Đã nộp, Chấm điểm...) của 1 bài tập siêu nhanh
-- Thay vì tạo index riêng cho status, ta gộp luôn assignment_id vào đầu
CREATE INDEX idx_submissions_stats ON assignment_submissions(assignment_id, submission_status);

-- Tối ưu cho việc lọc bài nộp muộn của 1 bài tập cụ thể
CREATE INDEX idx_submissions_late_check ON assignment_submissions(assignment_id, is_late);

-- Giúp tìm nhanh "Học sinh A đã làm những bài nào, trạng thái ra sao"
CREATE INDEX idx_submissions_student_stats ON assignment_submissions(student_id, submission_status);

-- =======================================================
-- 3. BẢNG TEACHING_ASSIGNMENTS (Phân công)
-- =======================================================

-- Tối ưu cho Dashboard Giáo viên
CREATE INDEX idx_teaching_dashboard ON teaching_assignments(teacher_id, school_year_id);

-- ============================================
-- HOÀN THÀNH
-- ============================================

SELECT 'Views và Stored Procedures cho thống kê đã được tạo thành công!' AS message;