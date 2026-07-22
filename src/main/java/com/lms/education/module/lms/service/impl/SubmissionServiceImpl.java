package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms.dto.SubmissionDto;
import com.lms.education.module.lms.entity.Assignment;
import com.lms.education.module.lms.entity.Submission;
import com.lms.education.module.lms.repository.AssignmentRepository;
import com.lms.education.module.lms.repository.SubmissionRepository;
import com.lms.education.module.lms.service.SubmissionService;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final TeachingSubstitutionRepository teachingSubstitutionRepository;

    @Override
    @Transactional
    public SubmissionDto startSubmission(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + assignmentId));

        if ("UNPUBLISHED".equalsIgnoreCase(assignment.getStatus()) || "DRAFT".equalsIgnoreCase(assignment.getStatus())) {
            throw new OperationNotPermittedException("Bài tập này chưa được phát hành, không thể làm bài!");
        }

        if ("CLOSED".equalsIgnoreCase(assignment.getStatus())) {
            throw new OperationNotPermittedException("Bài tập này đã đóng (CLOSED), không thể bắt đầu làm bài!");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);
        Student student = getCurrentStudent(currentUser);

        Optional<Submission> existingOpt = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, student.getId());
        if (existingOpt.isPresent()) {
            Submission existing = existingOpt.get();
            if ("SUBMITTED".equalsIgnoreCase(existing.getStatus())
                    || "LATE".equalsIgnoreCase(existing.getStatus())
                    || "GRADED".equalsIgnoreCase(existing.getStatus())) {
                throw new OperationNotPermittedException("Bạn đã nộp bài tập này rồi, không thể bắt đầu lại!");
            }
            log.info("Học viên {} tiếp tục làm bài tập ID: {}", student.getFullName(), assignmentId);
            return mapToDto(existing);
        }

        Submission submission = Submission.builder()
                .assignment(assignment)
                .student(student)
                .startTime(LocalDateTime.now())
                .status("IN_PROGRESS")
                .build();

        Submission saved = submissionRepository.save(submission);
        log.info("Học viên {} đã bắt đầu làm bài tập ID: {}", student.getFullName(), assignmentId);

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public SubmissionDto submitAssignment(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt làm bài với ID: " + submissionId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);
        Student student = getCurrentStudent(currentUser);

        if (!submission.getStudent().getId().equals(student.getId())) {
            throw new OperationNotPermittedException("Bạn không phải là chủ sở hữu lượt làm bài này!");
        }

        if (!"IN_PROGRESS".equalsIgnoreCase(submission.getStatus())) {
            throw new OperationNotPermittedException("Lượt làm bài này đã được nộp hoặc chấm điểm trước đó rồi!");
        }

        LocalDateTime now = LocalDateTime.now();
        submission.setEndTime(now);

        Assignment assignment = submission.getAssignment();
        if (assignment.getDueDate() != null && now.isAfter(assignment.getDueDate())) {
            submission.setStatus("LATE");
        } else {
            submission.setStatus("SUBMITTED");
        }

        Submission updated = submissionRepository.save(submission);
        log.info("Học viên {} đã nộp bài tập ID: {} với trạng thái: {}", student.getFullName(), assignment.getId(), updated.getStatus());

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public SubmissionDto gradeSubmission(Long submissionId, BigDecimal score, String feedback) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt làm bài với ID: " + submissionId));

        if ("IN_PROGRESS".equalsIgnoreCase(submission.getStatus())) {
            throw new OperationNotPermittedException("Học viên đang làm bài và chưa nộp bài, không thể chấm điểm!");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        // Kiểm tra phân công giảng dạy cho Lớp học chứa bài tập này
        checkTeacherClassPermission(currentUser, submission.getAssignment().getLesson().getClasses().getId());

        if (score != null) {
            if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("10.00")) > 0) {
                throw new OperationNotPermittedException("Điểm số (score) phải từ 0.00 đến 10.00!");
            }
            submission.setScore(score);
        }

        if (feedback != null) {
            submission.setFeedback(feedback.trim());
        }

        submission.setStatus("GRADED");

        Submission updated = submissionRepository.save(submission);
        log.info("Giảng viên đã chấm điểm lượt làm bài ID: {} (Điểm: {})", submissionId, score);

        return mapToDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDto getById(Long id) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt làm bài với ID: " + id));
        return mapToDto(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDto getMySubmission(Long assignmentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);
        Student student = getCurrentStudent(currentUser);

        Submission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa bắt đầu làm bài tập này!"));

        return mapToDto(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionDto> getByAssignmentId(Long assignmentId) {
        return submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionDto> getByStudentId(Long studentId) {
        return submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubmissionDto> getByAssignmentIdPageable(Long assignmentId, Pageable pageable) {
        return submissionRepository.findByAssignmentId(assignmentId, pageable).map(this::mapToDto);
    }

    private SubmissionDto mapToDto(Submission entity) {
        return SubmissionDto.builder()
                .id(entity.getId())
                .assignmentId(entity.getAssignment().getId())
                .assignmentTitle(entity.getAssignment().getTitle())
                .classId(entity.getAssignment().getLesson().getClasses().getId())
                .className(entity.getAssignment().getLesson().getClasses().getName())
                .studentId(entity.getStudent().getId())
                .studentCode(entity.getStudent().getStudentCode())
                .studentName(entity.getStudent().getFullName())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .score(entity.getScore())
                .feedback(entity.getFeedback())
                .status(entity.getStatus())
                .submittedAt(entity.getSubmittedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private User getCurrentUser(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    private Student getCurrentStudent(User currentUser) {
        if (currentUser == null) {
            throw new OperationNotPermittedException("Bạn cần đăng nhập tài khoản Học viên để thực hiện thao tác này!");
        }
        return studentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new OperationNotPermittedException("Tài khoản hiện tại chưa được liên kết hồ sơ Học viên!"));
    }

    private void checkTeacherClassPermission(User currentUser, Long classId) {
        if (classId == null) return;

        if (currentUser != null) {
            Long userId = currentUser.getId();
            Long staffId = staffRepository.findByUserId(userId).map(Staff::getId).orElse(null);

            boolean isAssigned = scheduleAssignmentRepository.isTeacherAssignedToClass(classId, userId, staffId);
            boolean isSubstituting = teachingSubstitutionRepository.isTeacherSubstitutingForClass(classId, userId, staffId);

            if (!isAssigned && !isSubstituting) {
                throw new OperationNotPermittedException("Bạn không được phân công giảng dạy lớp học này nên không có quyền chấm bài!");
            }
        }
    }
}
