package com.lms.education.module.assignments.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.assignments.dto.SubmissionAnswerDto;
import com.lms.education.module.assignments.entity.AssignmentQuestion;
import com.lms.education.module.assignments.entity.AssignmentSubmission;
import com.lms.education.module.assignments.entity.QuestionOption;
import com.lms.education.module.assignments.entity.SubmissionAnswer;
import com.lms.education.module.assignments.repository.AssignmentQuestionRepository;
import com.lms.education.module.assignments.repository.AssignmentSubmissionRepository;
import com.lms.education.module.assignments.repository.QuestionOptionRepository;
import com.lms.education.module.assignments.repository.SubmissionAnswerRepository;
import com.lms.education.module.assignments.service.SubmissionAnswerService;
import com.lms.education.module.notification.service.NotificationService;
import com.lms.education.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionAnswerServiceImpl implements SubmissionAnswerService {

    private final SubmissionAnswerRepository answerRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentQuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final com.lms.education.module.user.repository.UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public SubmissionAnswerDto saveAnswer(String submissionId, String questionId, SubmissionAnswerDto dto, String userId) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài nộp!"));

        // Validate quyền: Phải là bài của mình mới được lưu
        if (!submission.getStudent().getUser().getId().equals(userId)) {
            throw new OperationNotPermittedException("Bạn không có quyền sửa bài của người khác!");
        }

        // Chặn không cho sửa nếu đã nộp chính thức
        if (submission.getSubmissionStatus() != AssignmentSubmission.SubmissionStatus.draft &&
                submission.getSubmissionStatus() != AssignmentSubmission.SubmissionStatus.not_submitted) {
            throw new OperationNotPermittedException("Bài thi đã được nộp, không thể thay đổi đáp án!");
        }

        AssignmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi!"));

        // Tìm xem câu này đã từng lưu nháp chưa, chưa thì tạo mới
        SubmissionAnswer answer = answerRepository.findBySubmissionIdAndQuestionId(submissionId, questionId)
                .orElse(new SubmissionAnswer());

        answer.setSubmission(submission);
        answer.setQuestion(question);
        answer.setAnswerText(dto.getAnswerText()); // Lưu nội dung tự luận (nếu có)

        // Nếu là câu trắc nghiệm, gán option học sinh đã chọn
        if (dto.getSelectedOptionId() != null && !dto.getSelectedOptionId().isBlank()) {
            QuestionOption option = optionRepository.findById(dto.getSelectedOptionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Đáp án chọn không hợp lệ!"));
            answer.setSelectedOption(option);
        } else {
            answer.setSelectedOption(null);
        }

        return mapToDto(answerRepository.save(answer));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionAnswerDto> getAnswersBySubmission(String submissionId, String userId) {
        // Lấy thông tin bài nộp để biết ai là chủ nhân
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài nộp!"));

        // Lấy thông tin User đang request để lấy Role
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại!"));

        // Kiểm tra xem User này có phải là Học sinh không
        boolean isStudentRole = currentUser.getRole().getCode().equalsIgnoreCase("STUDENT");
        String ownerUserId = submission.getStudent().getUser().getId();

        // Nếu là Học sinh VÀ không phải là chủ nhân của bài nộp thì ko thể xem
        if (isStudentRole && !ownerUserId.equals(userId)) {
            throw new OperationNotPermittedException("Truy cập bị từ chối! Bạn không thể xem bài làm của học sinh khác.");
        }
        // (Nếu là Giáo viên hoặc Admin thì luồng code sẽ đi tiếp bình thường)

        // Nếu qua được khâu kiểm duyệt, lấy danh sách đáp án trả về
        List<SubmissionAnswer> answers = answerRepository.findBySubmissionIdOrderByQuestionOrderAsc(submissionId);
        return answers.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Double autoGradeSubmission(String submissionId) {
        log.info("Bắt đầu chấm điểm tự động cho bài nộp ID: {}", submissionId);

        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài nộp!"));

        List<SubmissionAnswer> answers = answerRepository.findBySubmissionIdOrderByQuestionOrderAsc(submissionId);
        double totalScore = 0.0;

        // Quét từng câu trả lời để tính điểm tự động cho phần Trắc nghiệm
        for (SubmissionAnswer answer : answers) {
            AssignmentQuestion question = answer.getQuestion();

            // Nếu câu hỏi này là Trắc nghiệm (multiple_choice)
            if (question.getQuestionType() == AssignmentQuestion.QuestionType.multiple_choice) {
                QuestionOption selected = answer.getSelectedOption();

                if (selected != null && Boolean.TRUE.equals(selected.getIsCorrect())) {
                    answer.setIsCorrect(true);
                    answer.setScore(question.getScore()); // Trả lời đúng -> Được full điểm câu đó
                    totalScore += question.getScore();
                } else {
                    answer.setIsCorrect(false);
                    answer.setScore(0.0); // Trả lời sai -> 0 điểm
                }
            }
            // Nếu là câu Tự luận
            else {
                answer.setIsCorrect(null); // Để null chờ giáo viên chấm
                answer.setScore(0.0);
            }
        }

        // Lưu lại kết quả của từng câu
        answerRepository.saveAll(answers);

        // Cập nhật tổng điểm hiện tại vào bảng bài nộp
        submission.setScore(totalScore);

        // DÙNG ENUM CỦA BẢNG ASSIGNMENT ĐỂ QUYẾT ĐỊNH TRẠNG THÁI BÀI NỘP
        String assignType = submission.getAssignment().getAssignmentType().name();

        if ("multiple_choice".equalsIgnoreCase(assignType)) {
            submission.setSubmissionStatus(AssignmentSubmission.SubmissionStatus.graded);
            submission.setGradingMethod(AssignmentSubmission.GradingMethod.auto);
            submission.setGradedAt(LocalDateTime.now());
        } else {
            // Nếu đề là 'mixed' (hỗn hợp), 'essay' (tự luận), hoặc 'file_upload' (nộp file)
            // -> Hệ thống chỉ chấm tạm phần trắc nghiệm, trạng thái vẫn giữ là 'submitted' chờ giáo viên
            submission.setSubmissionStatus(AssignmentSubmission.SubmissionStatus.submitted);
        }

        submissionRepository.save(submission);
        log.info("Đã chấm tự động xong bài {}. Loại bài: {}. Tổng điểm tạm tính: {}", submissionId, assignType, totalScore);

        // Chỉ gửi thông báo nếu bài tập đã được chấm xong 100% (chuyển sang trạng thái graded)
        if (submission.getSubmissionStatus() == AssignmentSubmission.SubmissionStatus.graded) {
            try {
                String studentUserId = submission.getStudent().getUser().getId();
                String assignmentTitle = submission.getAssignment().getTitle();
                Double finalScore = submission.getScore();

                notificationService.sendNotification(
                        studentUserId,                  // Người nhận là Học sinh
                        null,                           // Gửi bằng "Hệ thống" (truyền null)
                        "Có điểm bài tập mới!",
                        "Hệ thống đã tự động chấm xong bài tập '" + assignmentTitle + "'. Điểm số của bạn là: " + finalScore,
                        com.lms.education.module.notification.entity.Notification.NotificationType.grade,
                        "submissions",                  // Bảng liên quan
                        submission.getId(),             // ID bài nộp
                        "{\"score\": " + finalScore + "}" // JSON chứa điểm số
                );
            } catch (Exception e) {
                log.error("Lỗi khi gửi thông báo điểm tự động: ", e);
            }
        }

        return totalScore;
    }

    // --- Hàm Helper ---
    private SubmissionAnswerDto mapToDto(SubmissionAnswer entity) {
        return SubmissionAnswerDto.builder()
                .id(entity.getId())
                .submissionId(entity.getSubmission().getId())
                .questionId(entity.getQuestion().getId())
                .questionText(entity.getQuestion().getQuestionText())
                .answerText(entity.getAnswerText())
                .selectedOptionId(entity.getSelectedOption() != null ? entity.getSelectedOption().getId() : null)
                .isCorrect(entity.getIsCorrect())
                .score(entity.getScore())
                .feedback(entity.getFeedback())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}