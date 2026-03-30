package com.lms.education.module.assignments.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.assignments.dto.AssignmentQuestionDto;
import com.lms.education.module.assignments.dto.QuestionOptionDto;
import com.lms.education.module.assignments.entity.Assignment;
import com.lms.education.module.assignments.entity.AssignmentQuestion;
import com.lms.education.module.assignments.repository.AssignmentQuestionRepository;
import com.lms.education.module.assignments.repository.AssignmentRepository;
import com.lms.education.module.assignments.service.AssignmentQuestionService;
import com.lms.education.module.assignments.service.QuestionOptionService;
import com.lms.education.module.lms_class.entity.OnlineClass;
import com.lms.education.module.lms_class.repository.OnlineClassRepository;
import com.lms.education.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentQuestionServiceImpl implements AssignmentQuestionService {

    private final AssignmentQuestionRepository questionRepository;
    private final AssignmentRepository assignmentRepository;
    private final OnlineClassRepository onlineClassRepository;
    private final QuestionOptionService questionOptionService;

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

    @Override
    @Transactional
    public Map<String, Object> importFromExcel(String assignmentId, MultipartFile file, String userId) {
        // Kiểm tra quyền sở hữu bài tập
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + assignmentId));
        validateTeacherInClass(userId, assignment.getOnlineClass().getId());
        if (!assignment.getCreatedBy().getId().equals(userId)) {
            throw new OperationNotPermittedException("Bạn không có quyền thêm câu hỏi vào bài tập này!");
        }

        int successCount = 0;
        int currentOrder = (int) questionRepository.countByAssignmentId(assignmentId) + 1;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); // Đọc sheet đầu tiên

            // Bắt đầu từ dòng 1 (Bỏ qua dòng 0 là Tiêu đề cột)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || getCellValueAsString(row.getCell(0)).isBlank()) {
                    continue; // Bỏ qua dòng trống
                }

                // ĐỌC DỮ LIỆU TỪ EXCEL (Quy định cấu trúc file)
                String questionText = getCellValueAsString(row.getCell(0)); // Cột A: Nội dung câu hỏi
                String optionA = getCellValueAsString(row.getCell(1));      // Cột B: Đáp án A
                String optionB = getCellValueAsString(row.getCell(2));      // Cột C: Đáp án B
                String optionC = getCellValueAsString(row.getCell(3));      // Cột D: Đáp án C
                String optionD = getCellValueAsString(row.getCell(4));      // Cột E: Đáp án D

                // Cột F: Đáp án đúng (Ghi số 1, 2, 3 hoặc 4)
                String correctCol = getCellValueAsString(row.getCell(5));
                int correctOptionIndex = correctCol.isBlank() ? 1 : Integer.parseInt(correctCol);

                // Cột G: Điểm (Mặc định 1 điểm nếu không điền)
                String scoreCol = getCellValueAsString(row.getCell(6));
                double score = scoreCol.isBlank() ? 1.0 : Double.parseDouble(scoreCol);

                // TẠO CÂU HỎI MỚI VÀ LƯU DB
                AssignmentQuestion question = AssignmentQuestion.builder()
                        .assignment(assignment)
                        .questionOrder(currentOrder++)
                        .questionText(questionText)
                        .questionType(AssignmentQuestion.QuestionType.multiple_choice) // Giả định import trắc nghiệm 1 đáp án
                        .score(score)
                        .build();
                AssignmentQuestion savedQuestion = questionRepository.save(question);

                // CHUẨN BỊ DANH SÁCH ĐÁP ÁN
                List<QuestionOptionDto> options = new ArrayList<>();
                options.add(createOptionDto(1, optionA, correctOptionIndex == 1));
                options.add(createOptionDto(2, optionB, correctOptionIndex == 2));
                options.add(createOptionDto(3, optionC, correctOptionIndex == 3));
                options.add(createOptionDto(4, optionD, correctOptionIndex == 4));

                // Gọi Service lưu đáp án (Đã có sẵn validate chặt chẽ)
                questionOptionService.saveOptionsForQuestion(savedQuestion.getId(), options);

                successCount++;
            }

        } catch (Exception e) {
            log.error("Lỗi khi import file Excel: ", e);
            throw new RuntimeException("Quá trình import thất bại. Vui lòng kiểm tra lại định dạng file Excel! Lỗi: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Import thành công!");
        result.put("totalImported", successCount);
        return result;
    }

    // CÁC HÀM HELPER XỬ LÝ EXCEL VÀ DTO
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Ép kiểu số nguyên nếu không có phần thập phân (Ví dụ 1.0 -> "1")
                if (cell.getNumericCellValue() == Math.floor(cell.getNumericCellValue())) {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
                return String.valueOf(cell.getNumericCellValue());
            default:
                return "";
        }
    }

    private QuestionOptionDto createOptionDto(int order, String text, boolean isCorrect) {
        return QuestionOptionDto.builder()
                .displayOrder(order)
                .optionText(text.isBlank() ? "Đáp án trống" : text)
                .isCorrect(isCorrect)
                .build();
    }
}

