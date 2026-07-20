package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.ClassesDto;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.entity.Term;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.academic.repository.TermRepository;
import com.lms.education.module.academic.service.ClassesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassesServiceImpl implements ClassesService {

    private final ClassesRepository classesRepository;
    private final CourseRepository courseRepository;
    private final TermRepository termRepository;

    @Override
    @Transactional
    public ClassesDto create(ClassesDto dto) {
        String formattedCode = dto.getCode().trim().toUpperCase();

        if (classesRepository.existsByCode(formattedCode)) {
            throw new DuplicateResourceException("Mã lớp học '" + formattedCode + "' đã tồn tại trên hệ thống!");
        }

        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new OperationNotPermittedException("Ngày bắt đầu phải trước ngày kết thúc!");
        }

        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + dto.getCourseId()));

        Term term = null;
        if (dto.getTermId() != null) {
            term = termRepository.findById(dto.getTermId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ/đợt học với ID: " + dto.getTermId()));
        }

        Classes classes = Classes.builder()
                .code(formattedCode)
                .name(dto.getName().trim())
                .course(course)
                .term(term)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .maxStudents(dto.getMaxStudents() != null ? dto.getMaxStudents() : 20)
                .currentStudents(0)
                .status(dto.getStatus() != null ? dto.getStatus().trim().toUpperCase() : "OPENING")
                .build();

        Classes savedClass = classesRepository.save(classes);
        log.info("Đã tạo mới lớp học: {} (Mã: {})", savedClass.getName(), savedClass.getCode());

        return mapToDto(savedClass);
    }

    @Override
    @Transactional
    public ClassesDto update(Long id, ClassesDto dto) {
        Classes classes = classesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));

        String formattedCode = dto.getCode().trim().toUpperCase();

        if (!classes.getCode().equals(formattedCode) && classesRepository.existsByCode(formattedCode)) {
            throw new DuplicateResourceException("Mã lớp học '" + formattedCode + "' đã được sử dụng!");
        }

        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new OperationNotPermittedException("Ngày bắt đầu phải trước ngày kết thúc!");
        }

        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + dto.getCourseId()));

        Term term = null;
        if (dto.getTermId() != null) {
            term = termRepository.findById(dto.getTermId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ/đợt học với ID: " + dto.getTermId()));
        }

        classes.setCode(formattedCode);
        classes.setName(dto.getName().trim());
        classes.setCourse(course);
        classes.setTerm(term);
        classes.setStartDate(dto.getStartDate());
        classes.setEndDate(dto.getEndDate());
        if (dto.getMaxStudents() != null) {
            classes.setMaxStudents(dto.getMaxStudents());
        }
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            classes.setStatus(dto.getStatus().trim().toUpperCase());
        }

        Classes updatedClass = classesRepository.save(classes);
        log.info("Đã cập nhật lớp học ID: {}", id);

        return mapToDto(updatedClass);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Classes classes = classesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));

        classesRepository.delete(classes);
        log.info("Đã xóa hoàn toàn lớp học ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassesDto getById(Long id) {
        Classes classes = classesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));
        return mapToDto(classes);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassesDto> getAllClasses(String keyword, Pageable pageable) {
        Page<Classes> classes;
        if (keyword != null && !keyword.trim().isEmpty()) {
            classes = classesRepository.searchClasses(keyword.trim(), pageable);
        } else {
            classes = classesRepository.findAll(pageable);
        }
        return classes.map(this::mapToDto);
    }

    private ClassesDto mapToDto(Classes classes) {
        return ClassesDto.builder()
                .id(classes.getId())
                .courseId(classes.getCourse().getId())
                .courseCode(classes.getCourse().getCode())
                .courseName(classes.getCourse().getName())
                .termId(classes.getTerm() != null ? classes.getTerm().getId() : null)
                .termCode(classes.getTerm() != null ? classes.getTerm().getCode() : null)
                .termName(classes.getTerm() != null ? classes.getTerm().getName() : null)
                .code(classes.getCode())
                .name(classes.getName())
                .startDate(classes.getStartDate())
                .endDate(classes.getEndDate())
                .maxStudents(classes.getMaxStudents())
                .currentStudents(classes.getCurrentStudents())
                .status(classes.getStatus())
                .createdAt(classes.getCreatedAt())
                .updatedAt(classes.getUpdatedAt())
                .build();
    }
}
