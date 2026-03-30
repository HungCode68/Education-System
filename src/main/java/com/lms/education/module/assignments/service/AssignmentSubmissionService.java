package com.lms.education.module.assignments.service;

import com.lms.education.module.assignments.dto.AssignmentSubmissionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssignmentSubmissionService {

    // Học sinh lưu nháp hoặc nộp bài
    AssignmentSubmissionDto submitAssignment(String assignmentId, String studentId, String studentNote, boolean isSubmit);

    // Học sinh xem lại bài nộp của chính mình
    AssignmentSubmissionDto getMySubmission(String assignmentId, String studentId);

    // Giáo viên chấm điểm (Thủ công)
    AssignmentSubmissionDto gradeSubmission(String submissionId, String graderUserId, Double score, String feedback);

    // Giáo viên xem danh sách nộp bài của cả lớp
    Page<AssignmentSubmissionDto> searchSubmissionsByAssignment(
            String assignmentId, String keyword, com.lms.education.module.assignments.entity.AssignmentSubmission.SubmissionStatus status, Pageable pageable);
}
