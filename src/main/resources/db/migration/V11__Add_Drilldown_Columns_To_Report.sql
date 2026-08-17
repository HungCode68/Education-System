-- Thêm các cột lưu trữ danh sách ID dạng JSON vào bảng report_center_statistics để hỗ trợ xem chi tiết báo cáo
ALTER TABLE report_center_statistics
ADD COLUMN new_student_ids JSON,
ADD COLUMN dropped_student_ids JSON,
ADD COLUMN new_teacher_ids JSON,
ADD COLUMN resigned_teacher_ids JSON,
ADD COLUMN new_class_ids JSON,
ADD COLUMN closed_class_ids JSON;
