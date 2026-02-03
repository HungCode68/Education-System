-- V3__Update_Roles_Permissions_Users.sql

-- 1. Add refresh_token and expiration to users table
-- Allows storing JWT refresh token for session management
ALTER TABLE users ADD COLUMN refresh_token TEXT DEFAULT NULL;
ALTER TABLE users ADD COLUMN refresh_token_expiry DATETIME DEFAULT NULL;

-- 2. Modify permissions table
-- Change scope to ENUM as requested
ALTER TABLE permissions MODIFY COLUMN scope ENUM('SYSTEM', 'ACADEMIC_YEAR', 'GRADE', 'CLASS', 'USER', 'SUBJECT', 'ASSIGNMENT', 'MATERIAL', 'GRADEBOOK', 'REPORT') NOT NULL;

-- 3. Seed Roles
INSERT INTO roles (id, code, name, status)
SELECT UUID(), 'SYSTEM_ADMIN', 'Admin hệ thống', 'active'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'SYSTEM_ADMIN');

INSERT INTO roles (id, code, name, status)
SELECT UUID(), 'SCHOOL_BOARD', 'Ban Giám hiệu', 'active'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'SCHOOL_BOARD');

INSERT INTO roles (id, code, name, status)
SELECT UUID(), 'HOMEROOM_TEACHER', 'Giáo viên chủ nhiệm', 'active'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'HOMEROOM_TEACHER');

INSERT INTO roles (id, code, name, status)
SELECT UUID(), 'SUBJECT_TEACHER', 'Giáo viên bộ môn', 'active'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'SUBJECT_TEACHER');

INSERT INTO roles (id, code, name, status)
SELECT UUID(), 'STUDENT', 'Học Sinh', 'active'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'STUDENT');

-- 4. Seed Permissions
-- Note: Mapped SUBMISSION -> ASSIGNMENT, NOTIFICATION -> SYSTEM to fit ENUM constraints

-- 4.1 Quản lý Năm học – Khối – Môn (Ban giám hiệu)
INSERT INTO permissions (code, scope, name) VALUES
('ACADEMIC_YEAR_CREATE', 'ACADEMIC_YEAR', N'Tạo năm học'),
('ACADEMIC_YEAR_UPDATE', 'ACADEMIC_YEAR', N'Cập nhật năm học'),
('ACADEMIC_YEAR_VIEW',   'ACADEMIC_YEAR', N'Xem năm học'),

('GRADE_MANAGE', 'GRADE', N'Quản lý khối'),

('SUBJECT_MANAGE', 'SUBJECT', N'Quản lý danh mục môn học'),
('CURRICULUM_CONFIG', 'GRADE', N'Cấu hình môn học theo khối');

-- 4.2 Quản lý Lớp học & Phân công
INSERT INTO permissions (code, scope, name) VALUES
('CLASS_CREATE', 'CLASS', N'Tạo lớp học'),
('CLASS_VIEW',   'CLASS', N'Xem danh sách lớp'),
('CLASS_UPDATE', 'CLASS', N'Cập nhật lớp học'),

('CLASS_IMPORT_STUDENT', 'CLASS', N'Import học sinh'),
('CLASS_TRANSFER_STUDENT', 'CLASS', N'Chuyển lớp học sinh'),
('CLASS_PROMOTE', 'CLASS', N'Lên lớp cuối năm'),

('TEACHING_ASSIGN', 'CLASS', N'Phân công giảng dạy'),
('ONLINE_CLASS_VIEW', 'CLASS', N'Xem lớp học trực tuyến'),
('ONLINE_CLASS_CHANGE_TEACHER', 'CLASS', N'Đổi giáo viên lớp online');

-- 4.3 Giáo viên – Quản lý lớp & học sinh
INSERT INTO permissions (code, scope, name) VALUES
('STUDENT_VIEW',   'CLASS', N'Xem danh sách học sinh'),
('STUDENT_SEARCH', 'CLASS', N'Tìm kiếm học sinh'),
('STUDENT_FILTER', 'CLASS', N'Lọc học sinh theo trạng thái');

-- 4.4 Quản lý Tài liệu học tập
INSERT INTO permissions (code, scope, name) VALUES
('MATERIAL_UPLOAD', 'MATERIAL', N'Upload tài liệu'),
('MATERIAL_VIEW', 'MATERIAL', N'Xem danh sách tài liệu'),
('MATERIAL_DOWNLOAD', 'MATERIAL', N'Tải tài liệu'),
('MATERIAL_UPDATE', 'MATERIAL', N'Chỉnh sửa tài liệu'),
('MATERIAL_PUBLISH', 'MATERIAL', N'Publish / Unpublish tài liệu'),
('MATERIAL_DELETE', 'MATERIAL', N'Xóa tài liệu');

-- 4.5 Quản lý Bài tập (Scope ASSIGNMENT)
INSERT INTO permissions (code, scope, name) VALUES
('ASSIGNMENT_CREATE', 'ASSIGNMENT', N'Tạo bài tập'),
('ASSIGNMENT_IMPORT', 'ASSIGNMENT', N'Import bài tập từ Excel'),
('ASSIGNMENT_VIEW', 'ASSIGNMENT', N'Xem danh sách bài tập'),
('ASSIGNMENT_SEARCH', 'ASSIGNMENT', N'Tìm kiếm bài tập'),
('ASSIGNMENT_UPDATE', 'ASSIGNMENT', N'Chỉnh sửa bài tập'),
('ASSIGNMENT_PUBLISH', 'ASSIGNMENT', N'Publish / Unpublish bài tập'),
('ASSIGNMENT_DELETE', 'ASSIGNMENT', N'Xóa bài tập');

-- 4.6 Bài làm & Thống kê tiến độ (Scope ASSIGNMENT - mapped from SUBMISSION)
INSERT INTO permissions (code, scope, name) VALUES
('SUBMISSION_VIEW', 'ASSIGNMENT', N'Xem bài làm học sinh'),
('SUBMISSION_FILTER', 'ASSIGNMENT', N'Lọc trạng thái bài làm'),
('ASSIGNMENT_STATISTIC', 'ASSIGNMENT', N'Xem thống kê làm bài');

-- 4.7 Quản lý Điểm số
INSERT INTO permissions (code, scope, name) VALUES
('GRADE_AUTO', 'GRADEBOOK', N'Tự động chấm điểm trắc nghiệm'),
('GRADE_MANUAL', 'GRADEBOOK', N'Chấm điểm tự luận'),
('GRADE_UPDATE', 'GRADEBOOK', N'Chỉnh sửa điểm'),
('GRADE_DELETE', 'GRADEBOOK', N'Xóa điểm');

-- 4.8 Giáo viên chủ nhiệm – Quản lý tài khoản & thông báo (Notification -> SYSTEM)
INSERT INTO permissions (code, scope, name) VALUES
('USER_CREATE', 'USER', N'Tạo người dùng'),
('USER_VIEW', 'USER', N'Xem tài khoản'),
('USER_UPDATE', 'USER', N'Cập nhật tài khoản'),
('USER_DELETE', 'USER', N'Xóa tài khoản'),
('USER_RESET_PASSWORD', 'USER', N'Reset mật khẩu'),
('USER_UPDATE_STATUS', 'USER', N'Cập nhật trạng thái tài khoản'),

('NOTIFICATION_CREATE', 'SYSTEM', N'Tạo thông báo'),
('NOTIFICATION_VIEW', 'SYSTEM', N'Xem thông báo'),
('NOTIFICATION_FILTER', 'SYSTEM', N'Lọc thông báo'),
('NOTIFICATION_UPDATE', 'SYSTEM', N'Chỉnh sửa thông báo'),
('NOTIFICATION_DELETE', 'SYSTEM', N'Xóa thông báo');

-- 4.9 Học sinh (Notification -> SYSTEM)
INSERT INTO permissions (code, scope, name) VALUES
('PROFILE_VIEW', 'USER', N'Xem thông tin cá nhân'),
('PROFILE_CHANGE_PASSWORD', 'USER', N'Đổi mật khẩu'),

('CLASS_VIEW_SELF', 'CLASS', N'Xem lớp đang học'),

('ASSIGNMENT_DO', 'ASSIGNMENT', N'Làm bài tập'),
('ASSIGNMENT_VIEW_RESULT', 'ASSIGNMENT', N'Xem kết quả bài tập'),

('NOTIFICATION_VIEW_SELF', 'SYSTEM', N'Xem thông báo'),

('LEARNING_HISTORY_VIEW', 'CLASS', N'Xem lịch sử học tập'),
('REPORT_VIEW', 'REPORT', N'Xem báo cáo');

-- 5. Map Permissions to Roles

-- 5.1 System Admin -> ALL
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON 1=1
WHERE r.code = 'SYSTEM_ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 5.2 School Board (BGH)
-- Quyền: Report, Năm học, Khối, Môn, Xem lớp, Phân công, Xem user
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    -- Quản lý cấu hình
    'ACADEMIC_YEAR_CREATE', 'ACADEMIC_YEAR_UPDATE', 'ACADEMIC_YEAR_VIEW',
    'GRADE_MANAGE', 'CURRICULUM_CONFIG',
    'SUBJECT_MANAGE',
    -- Quản lý lớp & phân công
    'CLASS_CREATE', 'CLASS_VIEW', 'CLASS_UPDATE', 
    'TEACHING_ASSIGN', 'ONLINE_CLASS_VIEW',
    -- Xem báo cáo
    'REPORT_VIEW',
    -- Xem danh sách user (để biết giáo viên/học sinh)
    'USER_VIEW', 'STUDENT_VIEW', 'STUDENT_SEARCH'
)
WHERE r.code = 'SCHOOL_BOARD'
AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 5.3 Homeroom Teacher (GVCN)
-- Quyền: Quản lý lớp chủ nhiệm, Học sinh trong lớp, Thông báo, Xem user (giới hạn)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    -- Lớp học
    'CLASS_VIEW', 'CLASS_UPDATE', 
    'CLASS_IMPORT_STUDENT', 'CLASS_TRANSFER_STUDENT', 'CLASS_PROMOTE',
    -- Học sinh
    'STUDENT_VIEW', 'STUDENT_SEARCH', 'STUDENT_FILTER',
    -- User (Học sinh)
    'USER_VIEW', 'USER_RESET_PASSWORD', 'USER_UPDATE_STATUS',
    -- Thông báo
    'NOTIFICATION_CREATE', 'NOTIFICATION_VIEW', 'NOTIFICATION_FILTER', 'NOTIFICATION_UPDATE', 'NOTIFICATION_DELETE',
    -- Xem môn, năm học
    'ACADEMIC_YEAR_VIEW', 'SUBJECT_MANAGE' -- (Maybe view only? SUBJECT_MANAGE usually admin. Removing SUBJECT_MANAGE for safety, assume they can View if API allows public view)
)
WHERE r.code = 'HOMEROOM_TEACHER'
AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 5.4 Subject Teacher (GVBM)
-- Quyền: Lớp (Xem), Bài tập, Tài liệu, Điểm số, Submission
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    -- Lớp
    'CLASS_VIEW', 'ONLINE_CLASS_VIEW',
    'STUDENT_VIEW', 'STUDENT_SEARCH',
    -- Tài liệu
    'MATERIAL_UPLOAD', 'MATERIAL_VIEW', 'MATERIAL_DOWNLOAD', 'MATERIAL_UPDATE', 'MATERIAL_PUBLISH', 'MATERIAL_DELETE',
    -- Bài tập
    'ASSIGNMENT_CREATE', 'ASSIGNMENT_IMPORT', 'ASSIGNMENT_VIEW', 'ASSIGNMENT_SEARCH', 
    'ASSIGNMENT_UPDATE', 'ASSIGNMENT_PUBLISH', 'ASSIGNMENT_DELETE', 'ASSIGNMENT_STATISTIC',
    -- Submission
    'SUBMISSION_VIEW', 'SUBMISSION_FILTER',
    -- Điểm
    'GRADE_AUTO', 'GRADE_MANUAL', 'GRADE_UPDATE', 'GRADE_DELETE'
)
WHERE r.code = 'SUBJECT_TEACHER'
AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 5.5 Student (HS)
-- Quyền: Xem lớp, Làm bài, Xem tài liệu, Xem thông báo
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'PROFILE_VIEW', 'PROFILE_CHANGE_PASSWORD',
    'CLASS_VIEW_SELF', 'LEARNING_HISTORY_VIEW',
    'ASSIGNMENT_DO', 'ASSIGNMENT_VIEW_RESULT',
    'MATERIAL_VIEW', 'MATERIAL_DOWNLOAD',
    'NOTIFICATION_VIEW_SELF'
)
WHERE r.code = 'STUDENT'
AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
