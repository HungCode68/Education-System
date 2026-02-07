package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.ClassStudentDto;
import com.lms.education.module.academic.dto.AutoDistributeRequest;

import java.util.List;
import java.util.Map;

public interface ClassStudentService {

    // Thêm thủ công 1 học sinh (Dùng cho ca lẻ tẻ)
    ClassStudentDto addStudentToClass(ClassStudentDto dto);

    // Xóa học sinh khỏi lớp (Xóa nhầm)
    void removeStudentFromClass(String id);

    // Cập nhật trạng thái (Thôi học, chuyển trường)
    void updateStatus(String id, String status);

    // Lấy danh sách học sinh của 1 lớp
    List<ClassStudentDto> getStudentsByClass(String classId, String status);

    // --- CÁC TÍNH NĂNG TỰ ĐỘNG HÓA (TOOLS) ---

    // Phân lớp tự động (Chia đều học sinh vào các lớp)
    // Trả về Map báo cáo: "Lớp 10A1: thêm 45 em", "Lớp 10A2: thêm 45 em"...
    Map<String, String> autoDistributeStudents(AutoDistributeRequest request);

    // Lên lớp tự động (Sao chép danh sách lớp cũ -> lớp mới)
    // oldClassId: Lớp 10A1 năm ngoái
    // newClassId: Lớp 11A1 năm nay
    void promoteStudents(String oldClassId, String newClassId);
}
