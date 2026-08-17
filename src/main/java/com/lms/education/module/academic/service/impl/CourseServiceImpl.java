package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.CourseDto;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.academic.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public CourseDto create(CourseDto dto) {
        // Chuẩn hóa mã khóa học
        String formattedCode = dto.getCode().trim().toUpperCase();

        if (courseRepository.existsByCode(formattedCode)) {
            throw new DuplicateResourceException("Mã khóa học '" + formattedCode + "' đã tồn tại trên hệ thống!");
        }

        Course course = Course.builder()
                .code(formattedCode)
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .durationHours(dto.getDurationHours() != null ? dto.getDurationHours() : 0)
                .totalSessions(dto.getTotalSessions() != null ? dto.getTotalSessions() : 0)
                .sessionsPerWeek(dto.getSessionsPerWeek() != null ? dto.getSessionsPerWeek() : 0)
                .basePrice(dto.getBasePrice())
                .status(dto.getStatus() != null ? dto.getStatus().toUpperCase() : "ACTIVE")
                .metadata(dto.getMetadata()) // Map trực tiếp dữ liệu JSON
                .build();

        Course savedCourse = courseRepository.save(course);
        log.info("Đã tạo mới khóa học: {} - {}", savedCourse.getCode(), savedCourse.getName());

        return mapToDto(savedCourse);
    }

    @Override
    @Transactional
    public CourseDto update(Long id, CourseDto dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + id));

        String newFormattedCode = dto.getCode().trim().toUpperCase();

        // Kiểm tra xem có đổi mã khóa học không và mã mới có bị trùng không
        if (!course.getCode().equals(newFormattedCode) && courseRepository.existsByCode(newFormattedCode)) {
            throw new DuplicateResourceException("Mã khóa học '" + newFormattedCode + "' đã được sử dụng cho khóa học khác!");
        }

        course.setCode(newFormattedCode);
        course.setName(dto.getName().trim());
        course.setDescription(dto.getDescription());
        course.setDurationHours(dto.getDurationHours() != null ? dto.getDurationHours() : 0);
        course.setTotalSessions(dto.getTotalSessions() != null ? dto.getTotalSessions() : 0);
        course.setSessionsPerWeek(dto.getSessionsPerWeek() != null ? dto.getSessionsPerWeek() : 0);
        course.setBasePrice(dto.getBasePrice());

        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            course.setStatus(dto.getStatus().toUpperCase());
        }

        course.setMetadata(dto.getMetadata());

        Course updatedCourse = courseRepository.save(course);
        log.info("Đã cập nhật thông tin khóa học ID: {}", id);

        return mapToDto(updatedCourse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + id));

        /* * LƯU Ý MỞ RỘNG TƯƠNG TỰ PHÒNG BAN:
         * Khi bạn xây dựng bảng Lớp học (Class), bạn sẽ cần tiêm ClassRepository vào đây
         * để kiểm tra xem khóa học này có đang được sử dụng để mở lớp nào không trước khi xóa.
         * Nếu có lớp, chúng ta sẽ chặn xóa cứng.
         */

        courseRepository.delete(course);
        log.info("Đã xóa hoàn toàn khóa học ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDto getById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + id));
        return mapToDto(course);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDto getByCode(String code) {
        String formattedCode = code.trim().toUpperCase();
        Course course = courseRepository.findByCode(formattedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với mã: " + formattedCode));
        return mapToDto(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDto> getAllCourses(String keyword, Pageable pageable) {
        Page<Course> courses;
        if (keyword != null && !keyword.trim().isEmpty()) {
            courses = courseRepository.searchCourses(keyword.trim(), pageable);
        } else {
            courses = courseRepository.findAll(pageable);
        }
        return courses.map(this::mapToDto);
    }

    // --- Helper Method ---
    private CourseDto mapToDto(Course course) {
        return CourseDto.builder()
                .id(course.getId())
                .code(course.getCode())
                .name(course.getName())
                .description(course.getDescription())
                .durationHours(course.getDurationHours())
                .totalSessions(course.getTotalSessions())
                .sessionsPerWeek(course.getSessionsPerWeek())
                .basePrice(course.getBasePrice())
                .status(course.getStatus())
                .metadata(course.getMetadata())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}