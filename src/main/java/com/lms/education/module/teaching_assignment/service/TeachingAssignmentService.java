package com.lms.education.module.teaching_assignment.service;

import com.lms.education.module.teaching_assignment.dto.TeachingAssignmentDto;

import java.util.List;

public interface TeachingAssignmentService {

    // Phân công giáo viên dạy một môn cho một lớp trong học kỳ cụ thể.
    // Nếu chưa có ai dạy -> Tạo mới.
    // Nếu đã có người dạy -> Cập nhật (Thay thế giáo viên cũ bằng giáo viên mới).
    TeachingAssignmentDto assignTeacher(TeachingAssignmentDto dto);

    //Hủy phân công (Gỡ giáo viên khỏi lớp) @param assignmentId ID của bản ghi phân công cần xóa.
    void unassignTeacher(String assignmentId);

    // Lấy danh sách toàn bộ phân công của một lớp trong một học kỳ.
    // Dùng để hiển thị bảng "Giáo viên bộ môn" của lớp đó.
    List<TeachingAssignmentDto> getAssignmentsByClass(String classId, String semesterId);

    // Đếm số lớp mà một giáo viên đang dạy trong học kỳ.
    // Dùng để kiểm tra tải công việc (Workload) trước khi phân công thêm.
    long countTeacherWorkload(String teacherId, String semesterId);

    // Bạn có thể mở rộng thêm các hàm sau này nếu cần:
    // List<TeachingAssignmentDto> getAssignmentsByTeacher(String teacherId, String semesterId); // Xem lịch dạy của thầy A
}
