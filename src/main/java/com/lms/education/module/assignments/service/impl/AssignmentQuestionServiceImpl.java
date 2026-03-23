package com.lms.education.module.assignments.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.assignments.dto.AssignmentQuestionDto;
import com.lms.education.module.assignments.entity.Assignment;
import com.lms.education.module.assignments.entity.AssignmentQuestion;
import com.lms.education.module.assignments.repository.AssignmentQuestionRepository;
import com.lms.education.module.assignments.repository.AssignmentRepository;
import com.lms.education.module.assignments.service.AssignmentQuestionService;
import com.lms.education.module.lms_class.entity.OnlineClass;
import com.lms.education.module.lms_class.repository.OnlineClassRepository;
import com.lms.education.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentQuestionServiceImpl implements AssignmentQuestionService {

    private final AssignmentQuestionRepository questionRepository;
    private final AssignmentRepository assignmentRepository;
    private final OnlineClassRepository onlineClassRepository;

    @Override
    @Transactional
    public AssignmentQuestionDto create(AssignmentQuestionDto dto, String userId) {
        // Kiểm tra Bài tập gốc có tồn tại không
        Assignment assignment = assignmentRepository.findById(dto.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + dto.getAssignmentId()));

        // Kiểm tra xem User này có phải Giáo viên của lớp không
        validateTeacherInClass(userId, assignment.getOnlineClass().getId());

        // Chỉ người tạo ra bài tập mới được phép thêm câu hỏi vào bài tập đó
        if (!assignment.getCreatedBy().getId().equals(userId)) {
            throw new OperationNotPermittedException("Bạn không có quyền thêm câu hỏi vào bài tập của giáo viên khác!");
        }

        // Tự động tính số thứ tự câu hỏi tiếp theo nếu Frontend không gửi lên
        Integer order = dto.getQuestionOrder();
        if (order == null) {
            long currentCount = questionRepository.countByAssignmentId(assignment.getId());
            order = (int) currentCount + 1;
        }

        // Build Entity
        AssignmentQuestion question = AssignmentQuestion.builder()
                .assignment(assignment)
                .questionOrder(order)
                .questionOrder(dto.getQuestionOrder())
                .questionText(dto.getQuestionText())
                .explanation(dto.getExplanation())
                .questionType(dto.getQuestionType())
                .score(dto.getScore())
                .build();

        // Lưu và trả về
        AssignmentQuestion savedQuestion = questionRepository.save(question);
        log.info("Đã thêm câu hỏi thứ tự {} vào bài tập ID: {}", savedQuestion.getQuestionOrder(), assignment.getId());

        return mapToDto(savedQuestion);
    }

    @Override
    @Transactional
    public AssignmentQuestionDto update(String id, AssignmentQuestionDto dto, String userId) {
        AssignmentQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + id));

        Assignment assignment = question.getAssignment();

        // Kiểm tra quyền lớp học và quyền sở hữu bài tập
        validateTeacherInClass(userId, assignment.getOnlineClass().getId());
        if (!assignment.getCreatedBy().getId().equals(userId)) {
            throw new OperationNotPermittedException("Bạn không có quyền chỉnh sửa câu hỏi trong bài tập của giáo viên khác!");
        }

        // Cập nhật các trường thông tin
        question.setQuestionOrder(dto.getQuestionOrder());
        question.setQuestionText(dto.getQuestionText());
        question.setExplanation(dto.getExplanation());
        question.setQuestionType(dto.getQuestionType());
        question.setScore(dto.getScore());

        AssignmentQuestion updatedQuestion = questionRepository.save(question);
        log.info("Đã cập nhật câu hỏi ID: {}", id);

        return mapToDto(updatedQuestion);
    }

    @Override
    @Transactional
    public void delete(String id, String userId) {
        AssignmentQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + id));

        Assignment assignment = question.getAssignment();

        // Kiểm tra quyền lớp học và quyền sở hữu bài tập
        validateTeacherInClass(userId, assignment.getOnlineClass().getId());
        if (!assignment.getCreatedBy().getId().equals(userId)) {
            throw new OperationNotPermittedException("Bạn không có quyền xóa câu hỏi trong bài tập của giáo viên khác!");
        }

        questionRepository.delete(question);
        log.info("Đã xóa câu hỏi ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentQuestionDto getById(String id) {
        AssignmentQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + id));
        return mapToDto(question);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentQuestionDto> getByAssignmentId(String assignmentId) {
        // Gọi hàm repository để lấy danh sách đã được sắp xếp tăng dần theo question_order
        List<AssignmentQuestion> questions = questionRepository.findByAssignmentIdOrderByQuestionOrderAsc(assignmentId);

        return questions.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private void validateTeacherInClass(String userId, String classId) {
        OnlineClass onlineClass = onlineClassRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Lớp học với ID: " + classId));

        boolean isAssignedTeacher = false;
        if (onlineClass.getTeachingAssignment() != null
                && onlineClass.getTeachingAssignment().getTeacher() != null) {

            // từ Hồ sơ Giáo viên sang Tài khoản Đăng nhập (User)
            User teacherAccount = onlineClass.getTeachingAssignment().getTeacher().getUser();

            if (teacherAccount != null && teacherAccount.getId() != null) {
                String assignedTeacherUserId = teacherAccount.getId();

                if (assignedTeacherUserId.trim().equalsIgnoreCase(userId.trim())) {
                    isAssignedTeacher = true;
                }
            }
        }

        if (!isAssignedTeacher) {
            throw new OperationNotPermittedException("Truy cập bị từ chối! Chỉ Giáo viên phụ trách lớp mới có quyền thực hiện thao tác này.");
        }
    }

    // --- Hàm Helper ---
    private AssignmentQuestionDto mapToDto(AssignmentQuestion question) {
        return AssignmentQuestionDto.builder()
                .id(question.getId())
                .assignmentId(question.getAssignment().getId()) // Lấy ID của bài tập gốc
                .questionOrder(question.getQuestionOrder())
                .questionText(question.getQuestionText())
                .explanation(question.getExplanation())
                .questionType(question.getQuestionType())
                .score(question.getScore())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
}
