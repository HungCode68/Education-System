package com.lms.education.module.lms_class.service;

import com.lms.education.module.lms_class.dto.OnlineClassStudentDto;
import com.lms.education.module.lms_class.entity.OnlineClassStudent;

import java.util.List;

public interface OnlineClassStudentService {

    // Giáo viên thêm học sinh thủ công (VD: Học sinh học ghép)
    OnlineClassStudentDto addStudentManual(String onlineClassId, String studentId);

    // Xóa học sinh khỏi lớp (Soft delete: Chuyển status -> removed)
    void removeStudent(String onlineClassId, String studentId);

    // Lấy danh sách học sinh của một lớp (Cho GV xem)
    List<OnlineClassStudentDto> getStudentsByClass(String onlineClassId, OnlineClassStudent.StudentStatus status);

    // Lấy danh sách lớp của một học sinh (Cho HS xem Dashboard)
    List<OnlineClassStudentDto> getClassesByStudent(String studentId);

    // Đồng bộ học sinh từ lớp Vật lý sang lớp Online
    // Hàm này sẽ được gọi tự động sau khi tạo OnlineClass xong
    void syncStudentsFromPhysicalClass(String onlineClassId);
}
