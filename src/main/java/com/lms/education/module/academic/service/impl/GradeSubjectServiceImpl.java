package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.GradeSubjectDto;
import com.lms.education.module.academic.entity.Grade;
import com.lms.education.module.academic.entity.GradeSubject;
import com.lms.education.module.academic.entity.Subject;
import com.lms.education.module.academic.repository.GradeRepository;
import com.lms.education.module.academic.repository.GradeSubjectRepository;
import com.lms.education.module.academic.repository.SubjectRepository;
import com.lms.education.module.academic.service.GradeSubjectService;
import com.lms.education.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeSubjectServiceImpl implements GradeSubjectService {

    private final GradeSubjectRepository gradeSubjectRepository;
    private final GradeRepository gradeRepository;
    private final SubjectRepository subjectRepository;

    @Override
    @Transactional
    public GradeSubjectDto create(GradeSubjectDto dto) {
        log.info("Thêm môn học {} vào khối {}", dto.getSubjectId(), dto.getGradeId());

        // Validate: Khối và Môn phải tồn tại
        Grade grade = gradeRepository.findById(dto.getGradeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Khối với ID: " + dto.getGradeId()));

        if (!grade.getIsActive()) {
            throw new OperationNotPermittedException("Khối '" + grade.getName() + "' đang bị vô hiệu hóa, không thể thêm chương trình học!");
        }

        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Môn học với ID: " + dto.getSubjectId()));

        if (!subject.getIsActive()) {
            throw new OperationNotPermittedException("Môn học '" + subject.getName() + "' đang bị vô hiệu hóa, không thể thêm vào chương trình!");
        }

        // Validate: Trùng lặp (Môn này đã có trong khối chưa?)
        if (gradeSubjectRepository.existsByGradeIdAndSubjectId(dto.getGradeId(), dto.getSubjectId())) {
            throw new DuplicateResourceException("Môn '" + subject.getName() + "' đã có trong '" + grade.getName() + "'!");
        }

        // Map & Save
        GradeSubject entity = GradeSubject.builder()
                .grade(grade)
                .subject(subject)
                .subjectType(dto.getSubjectType() != null ? dto.getSubjectType() : GradeSubject.SubjectType.required)
                .isLmsEnabled(dto.getIsLmsEnabled() != null ? dto.getIsLmsEnabled() : true)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .build();

        return mapToDto(gradeSubjectRepository.save(entity));
    }

    @Override
    @Transactional
    public GradeSubjectDto update(String id, GradeSubjectDto dto) {
        log.info("Cập nhật cấu hình GradeSubject ID: {}", id);

        GradeSubject entity = gradeSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình với ID: " + id));

        // Lưu ý: Thường ta không cho phép sửa gradeId hay subjectId ở đây
        // (Nếu muốn đổi môn khác thì nên xóa đi tạo mới).
        // Ở đây chỉ cập nhật các thuộc tính cấu hình.

        if (dto.getSubjectType() != null) {
            entity.setSubjectType(dto.getSubjectType());
        }
        if (dto.getIsLmsEnabled() != null) {
            entity.setIsLmsEnabled(dto.getIsLmsEnabled());
        }
        if (dto.getDisplayOrder() != null) {
            entity.setDisplayOrder(dto.getDisplayOrder());
        }

        return mapToDto(gradeSubjectRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.info("Xóa cấu hình GradeSubject ID: {}", id);
        if (!gradeSubjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy cấu hình với ID: " + id);
        }
        gradeSubjectRepository.deleteById(id);
    }

    @Override
    public GradeSubjectDto getById(String id) {
        GradeSubject entity = gradeSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình với ID: " + id));
        return mapToDto(entity);
    }

    @Override
    public PageResponse<GradeSubjectDto> search(String gradeId, String keyword, int page, int size) {
        // Page bắt đầu từ 0
        Pageable pageable = PageRequest.of(page - 1, size);

        // Gọi query trong Repository
        Page<GradeSubject> pageResult = gradeSubjectRepository.search(gradeId, keyword, pageable);

        List<GradeSubjectDto> dtos = pageResult.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PageResponse.<GradeSubjectDto>builder()
                .content(dtos)
                .pageNo(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public List<GradeSubjectDto> getByGradeId(String gradeId, boolean onlyLmsEnabled) {
        Boolean filterLms = onlyLmsEnabled ? true : null;

        // Gọi hàm Query
        List<GradeSubject> entities = gradeSubjectRepository.findByGradeIdAndStatus(gradeId, filterLms);

        return entities.stream()
                .map(this::mapToDto)
                .toList();
    }

    // --- Helper Mapper ---
    private GradeSubjectDto mapToDto(GradeSubject entity) {
        return GradeSubjectDto.builder()
                .id(entity.getId())
                .gradeId(entity.getGrade().getId())
                .gradeName(entity.getGrade().getName())
                .subjectId(entity.getSubject().getId())
                .subjectName(entity.getSubject().getName())
                .subjectType(entity.getSubjectType())
                .isLmsEnabled(entity.getIsLmsEnabled())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
