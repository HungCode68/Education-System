package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.ClassTransferHistoryDto;
import com.lms.education.module.academic.dto.TransferStudentRequest;
import com.lms.education.utils.PageResponse;

import java.time.LocalDate;
import java.util.List;

public interface ClassTransferService {

    // Nghiệp vụ quan trọng: Thực hiện chuyển lớp
    void transferStudent(TransferStudentRequest request);

    // Xem lịch sử của 1 học sinh
    List<ClassTransferHistoryDto> getHistoryByStudent(String studentId);

    // Tìm kiếm lịch sử (Admin)
    PageResponse<ClassTransferHistoryDto> searchHistory(String keyword, String classId, LocalDate startDate, LocalDate endDate, int page, int size);
}
