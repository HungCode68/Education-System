package com.lms.education.module.assignments.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.assignments.dto.AssignmentSubmissionDto;
import com.lms.education.module.assignments.entity.Assignment;
import com.lms.education.module.assignments.repository.AssignmentRepository;
import com.lms.education.module.assignments.repository.AssignmentSubmissionRepository;
import com.lms.education.module.assignments.service.AssignmentSubmissionService;
import com.lms.education.module.assignments.service.SubmissionAnswerService;
import com.lms.education.module.notification.entity.Notification;
import com.lms.education.module.notification.service.NotificationService;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentSubmissionServiceImpl implements AssignmentSubmissionService {

    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final SubmissionAnswerService submissionAnswerService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AssignmentSubmissionDto submitAssignment(String assignmentId, String studentId, String studentNote, boolean isSubmit) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập!"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ học sinh!"));

        // Kiểm tra xem học sinh đã có bản ghi nộp bài nào chưa
        Optional<com.lms.education.module.assignments.entity.AssignmentSubmission> existingOpt = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);

        com.lms.education.module.assignments.entity.AssignmentSubmission submission;
        if (existingOpt.isPresent()) {
            submission = existingOpt.get();

            // CHẶN ĐỨNG NẾU TRẠNG THÁI KHÁC "NHÁP" (draft) VÀ "CHƯA NỘP" (not_submitted)
            if (submission.getSubmissionStatus() != com.lms.education.module.assignments.entity.AssignmentSubmission.SubmissionStatus.draft &&
                    submission.getSubmissionStatus() != com.lms.education.module.assignments.entity.AssignmentSubmission.SubmissionStatus.not_submitted) {
                throw new OperationNotPermittedException("Lỗi: Bài tập này đã được nộp chính thức. Bạn không thể chỉnh sửa hay nộp lại!");
            }

            // Tăng số lần thử (Attempt) nếu đây là lần nộp mới (không phải lưu nháp)
            if (isSubmit && submission.getSubmissionStatus() == com.lms.education.module.assignments.entity.AssignmentSubmission.SubmissionStatus.draft) {
                submission.setAttemptCount(submission.getAttemptCount() + 1);
            }
        } else {
            // Nếu chưa có thì tạo mới
            submission = com.lms.education.module.assignments.entity.AssignmentSubmission.builder()
                    .assignment(assignment)
                    .student(student)
                    .attemptCount(isSubmit ? 1 : 0)
                    .build();
        }

        // Cập nhật thông tin nộp bài
        submission.setStudentNote(studentNote);

        if (isSubmit) {
            LocalDateTime now = LocalDateTime.now();
            submission.setSubmittedAt(now);

            // LOGIC KIỂM TRA NỘP MUỘN
            boolean isLate = assignment.getDueTime() != null && now.isAfter(assignment.getDueTime());
            submission.setIsLate(isLate);

            submission.setSubmissionStatus(isLate ? com.lms.education.module.assignments.entity.AssignmentSubmission.SubmissionStatus.late : com.lms.education.module.assignments.entity.AssignmentSubmission.SubmissionStatus.submitted);
            log.info("Học sinh {} đã NỘP bài tập {}. Trạng thái muộn: {}", student.getStudentCode(), assignmentId, isLate);

            submissionRepository.save(submission); // Lưu trạng thái nộp bài trước
            submissionAnswerService.autoGradeSubmission(submission.getId()); // Bắt máy tự động chấm điểm luôn!
            return mapToDto(submission);
        } else {
            submission.setSubmissionStatus(com.lms.education.module.assignments.entity.AssignmentSubmission.SubmissionStatus.draft);
            log.info("Học sinh {} đã LƯU NHÁP bài tập {}", student.getStudentCode(), assignmentId);
        }

        return mapToDto(submissionRepository.save(submission));
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentSubmissionDto getMySubmission(String assignmentId, String studentId) {
        com.lms.education.module.assignments.entity.AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa có lịch sử làm bài tập này."));
        return mapToDto(submission);
    }

    @Override
    @Transactional
    public AssignmentSubmissionDto gradeSubmission(String submissionId, String graderUserId, Double score, String feedback) {
        com.lms.education.module.assignments.entity.AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài nộp với ID: " + submissionId));

        User grader = userRepository.findById(graderUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người chấm bài!"));

        // Cập nhật điểm và lời phê
        submission.setScore(score);
        submission.setTeacherFeedback(feedback);
        submission.setGradedBy(grader);
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradingMethod(com.lms.education.module.assignments.entity.AssignmentSubmission.GradingMethod.manual);
        submission.setSubmissionStatus(com.lms.education.module.assignments.entity.AssignmentSubmission.SubmissionStatus.graded);

        log.info("Giáo viên {} đã chấm điểm bài nộp {}. Điểm: {}", grader.getEmail(), submissionId, score);

        submissionRepository.save(submission);

        try {
            String studentUserId = submission.getStudent().getUser().getId();
            String assignmentTitle = submission.getAssignment().getTitle();
            Double finalScore = submission.getScore();

            notificationService.sendNotification(
                    studentUserId,
                    graderUserId,
                    "Có điểm bài tập mới!",
                    "Bài tập '" + assignmentTitle + "' của bạn đã được chấm. Điểm số: " + finalScore,
                    Notification.NotificationType.grade,
                    "submissions",
                    submission.getId(),
                    "{\"score\": " + finalScore + "}"
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo có điểm mới: ", e);
        }

        return mapToDto(submission);

    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentSubmissionDto> searchSubmissionsByAssignment(String assignmentId, String keyword, com.lms.education.module.assignments.entity.AssignmentSubmission.SubmissionStatus status, Pageable pageable) {
        return submissionRepository.searchSubmissionsByAssignment(assignmentId, keyword, status, pageable)
                .map(this::mapToDto);
    }

    // --- Hàm Helper Map Entity to DTO ---
    private AssignmentSubmissionDto mapToDto(com.lms.education.module.assignments.entity.AssignmentSubmission entity) {
        AssignmentSubmissionDto dto = AssignmentSubmissionDto.builder()
                .id(entity.getId())
                .assignmentId(entity.getAssignment().getId())
                .assignmentTitle(entity.getAssignment().getTitle())
                .studentId(entity.getStudent().getId())
                .studentName(entity.getStudent().getFullName())
                .studentCode(entity.getStudent().getStudentCode())
                .studentNote(entity.getStudentNote())
                .submissionStatus(entity.getSubmissionStatus())
                .submittedAt(entity.getSubmittedAt())
                .isLate(entity.getIsLate())
                .attemptCount(entity.getAttemptCount())
                .score(entity.getScore())
                .teacherFeedback(entity.getTeacherFeedback())
                .gradedAt(entity.getGradedAt())
                .gradingMethod(entity.getGradingMethod())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        if (entity.getGradedBy() != null) {
            dto.setGradedById(entity.getGradedBy().getId());
            dto.setGradedByName(entity.getGradedBy().getEmail()); // Hoặc tên giáo viên nếu bạn nối bảng Teacher
        }

        return dto;
    }
}
