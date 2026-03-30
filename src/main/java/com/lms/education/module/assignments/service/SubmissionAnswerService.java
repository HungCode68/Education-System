package com.lms.education.module.assignments.service;
import com.lms.education.module.assignments.dto.SubmissionAnswerDto;

import java.util.List;

public interface SubmissionAnswerService {

    // Học sinh lưu nháp đáp án cho 1 câu hỏi cụ thể (Gọi liên tục khi đang làm bài)
    SubmissionAnswerDto saveAnswer(String submissionId, String questionId, SubmissionAnswerDto dto, String userId);

    // Lấy toàn bộ bài làm của 1 học sinh (Để hiển thị lại lúc ôn tập hoặc cho giáo viên xem)
    List<SubmissionAnswerDto> getAnswersBySubmission(String submissionId, String userId);

    // HỆ THỐNG TỰ ĐỘNG CHẤM ĐIỂM (Sẽ được gọi tự động khi học sinh bấm Nộp bài)
    Double autoGradeSubmission(String submissionId);
}
