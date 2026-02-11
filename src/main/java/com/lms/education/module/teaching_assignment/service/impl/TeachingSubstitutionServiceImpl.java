package com.lms.education.module.teaching_assignment.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.teaching_assignment.dto.TeachingSubstitutionDto;
import com.lms.education.module.teaching_assignment.entity.TeachingAssignment;
import com.lms.education.module.teaching_assignment.entity.TeachingSubstitution;
import com.lms.education.module.teaching_assignment.repository.TeachingAssignmentRepository;
import com.lms.education.module.teaching_assignment.repository.TeachingSubstitutionRepository;
import com.lms.education.module.teaching_assignment.service.TeachingSubstitutionService;
import com.lms.education.module.user.entity.Teacher;
import com.lms.education.module.user.repository.TeacherRepository;
import com.lms.education.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeachingSubstitutionServiceImpl implements TeachingSubstitutionService {

    private final TeachingSubstitutionRepository substitutionRepository;
    private final TeachingAssignmentRepository assignmentRepository;
    private final TeacherRepository teacherRepository;

    @Override
    @Transactional
    public TeachingSubstitutionDto create(TeachingSubstitutionDto dto) {
        log.info("Tạo yêu cầu dạy thay: AssignmentId={} -> SubTeacherId={}", dto.getAssignmentId(), dto.getSubTeacherId());

        // Validate dữ liệu đầu vào
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }
        if (dto.getStartDate().isBefore(LocalDate.now())) {
            // Tùy nghiệp vụ: Có cho phép nhập lại lịch quá khứ không?
            // Ở đây mình warning nhẹ hoặc cho qua nếu nhập bù.
        }

        // Lấy thông tin Phân công gốc
        TeachingAssignment originalAssignment = assignmentRepository.findById(dto.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Phân công gốc không tồn tại"));

        if (!TeachingAssignment.AssignmentStatus.active.equals(originalAssignment.getStatus())) {
            throw new OperationNotPermittedException("Không thể tạo dạy thay cho một phân công đã ngưng hoạt động.");
        }

        // Lấy thông tin Giáo viên dạy thay
        Teacher subTeacher = teacherRepository.findById(dto.getSubTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên dạy thay không tồn tại"));

        // Validate: Không được tự dạy thay cho chính mình
        if (originalAssignment.getTeacher().getId().equals(subTeacher.getId())) {
            throw new OperationNotPermittedException("Giáo viên dạy thay trùng với giáo viên chính thức!");
        }

        // KIỂM TRA TRÙNG LỊCH (QUAN TRỌNG NHẤT)

        // Check A: Lớp này đã có ai dạy thay trong khoảng thời gian này chưa?
        if (substitutionRepository.existsOverlapForAssignment(
                dto.getAssignmentId(), dto.getStartDate(), dto.getEndDate(), null)) {
            throw new OperationNotPermittedException(
                    "Đã có người dạy thay khác cho lớp này trong khoảng thời gian từ " +
                            dto.getStartDate() + " đến " + dto.getEndDate()
            );
        }

        // Check B: Giáo viên dạy thay có bị kẹt lịch dạy thay ở lớp khác không?
        // (Lưu ý: Logic này chưa check lịch dạy chính thức của GV dạy thay - cái đó cần module Thời khóa biểu chi tiết hơn)
//        if (substitutionRepository.existsOverlapForSubTeacher(
//                subTeacher.getId(), dto.getStartDate(), dto.getEndDate(), null)) {
//            throw new OperationNotPermittedException(
//                    "Giáo viên " + subTeacher.getFullName() + " đã có lịch dạy thay lớp khác trong khoảng thời gian này."
//            );
//        }

        // Save
        TeachingSubstitution entity = TeachingSubstitution.builder()
                .originalAssignment(originalAssignment)
                .subTeacher(subTeacher)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .status(TeachingSubstitution.SubstitutionStatus.approved) // Mặc định Approved nếu Admin tạo
                .build();

        return mapToDto(substitutionRepository.save(entity));
    }

    @Override
    @Transactional
    public void cancel(String id) {
        TeachingSubstitution entity = substitutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu dạy thay không tồn tại"));

        entity.setStatus(TeachingSubstitution.SubstitutionStatus.cancelled);
        substitutionRepository.save(entity);
        log.info("Đã hủy dạy thay ID: {}", id);
    }

    @Override
    @Transactional
    public void updateStatus(String id, TeachingSubstitution.SubstitutionStatus status) {
        TeachingSubstitution entity = substitutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu dạy thay không tồn tại"));
        entity.setStatus(status);
        substitutionRepository.save(entity);
    }

    @Override
    public PageResponse<TeachingSubstitutionDto> search(String schoolYearId, String semesterId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<TeachingSubstitution> pageResult = substitutionRepository.search(schoolYearId, semesterId, keyword, pageable);

        List<TeachingSubstitutionDto> dtos = pageResult.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PageResponse.<TeachingSubstitutionDto>builder()
                .content(dtos)
                .pageNo(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public List<TeachingSubstitutionDto> getBySubTeacher(String teacherId) {
        return substitutionRepository.findBySubTeacherId(teacherId).stream()
                .map(this::mapToDto)
                .toList();
    }

    // --- MAPPER (Flatten Data) ---
    private TeachingSubstitutionDto mapToDto(TeachingSubstitution entity) {
        TeachingAssignment original = entity.getOriginalAssignment();

        return TeachingSubstitutionDto.builder()
                .id(entity.getId())

                // Info cơ bản
                .assignmentId(original.getId())
                .subTeacherId(entity.getSubTeacher().getId())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())

                // Info hiển thị (Flatten)
                .subTeacherName(entity.getSubTeacher().getFullName())
                .subTeacherCode(entity.getSubTeacher().getTeacherCode())

                .originalTeacherId(original.getTeacher().getId())
                .originalTeacherName(original.getTeacher().getFullName())
                .originalTeacherCode(original.getTeacher().getTeacherCode())

                .physicalClassId(original.getPhysicalClass().getId())
                .physicalClassName(original.getPhysicalClass().getName())

                .subjectId(original.getSubject().getId())
                .subjectName(original.getSubject().getName())

                .build();
    }
}
