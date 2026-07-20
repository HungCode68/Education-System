package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.lms.dto.LessonDto;
import com.lms.education.module.lms.entity.Lesson;
import com.lms.education.module.lms.repository.LessonRepository;
import com.lms.education.module.lms.service.LessonService;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final ClassesRepository classesRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final TeachingSubstitutionRepository teachingSubstitutionRepository;

    @Override
    @Transactional
    public LessonDto create(LessonDto dto) {
        Classes classes = classesRepository.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + dto.getClassId()));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        // Kiểm tra xem người dùng hiện tại có được phân công giảng dạy hoặc dạy thay cho lớp học này hay không
        checkTeacherClassPermission(currentUser, dto.getClassId());

        if (lessonRepository.existsByClassesIdAndOrderNumber(dto.getClassId(), dto.getOrderNumber())) {
            throw new DuplicateResourceException("Số thứ tự bài học này đã tồn tại trong lớp học!");
        }

        Lesson lesson = Lesson.builder()
                .classes(classes)
                .name(dto.getName().trim())
                .orderNumber(dto.getOrderNumber())
                .description(dto.getDescription())
                .build();

        Lesson saved = lessonRepository.save(lesson);
        log.info("Đã tạo thành công bài học: {} (STT: {}) cho lớp: {}", saved.getName(), saved.getOrderNumber(), classes.getName());

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public LessonDto update(Long id, LessonDto dto) {
        Lesson existing = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học với ID: " + id));

        Classes classes = classesRepository.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + dto.getClassId()));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        // Kiểm tra phân công lớp học cũ và lớp học mới
        checkTeacherClassPermission(currentUser, existing.getClasses().getId());
        checkTeacherClassPermission(currentUser, dto.getClassId());

        if ((!existing.getOrderNumber().equals(dto.getOrderNumber()) || !existing.getClasses().getId().equals(dto.getClassId()))
                && lessonRepository.existsByClassesIdAndOrderNumber(dto.getClassId(), dto.getOrderNumber())) {
            throw new DuplicateResourceException("Số thứ tự bài học này đã tồn tại trong lớp học!");
        }

        existing.setClasses(classes);
        existing.setName(dto.getName().trim());
        existing.setOrderNumber(dto.getOrderNumber());
        existing.setDescription(dto.getDescription());

        Lesson updated = lessonRepository.save(existing);
        log.info("Đã cập nhật bài học ID: {}", id);

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Lesson existing = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học với ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        // Kiểm tra phân công giảng dạy trước khi xóa bài học của lớp
        checkTeacherClassPermission(currentUser, existing.getClasses().getId());

        lessonRepository.delete(existing);
        log.info("Đã xóa bài học ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDto getById(Long id) {
        Lesson existing = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học với ID: " + id));
        return mapToDto(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonDto> getAll(String keyword, Pageable pageable) {
        Page<Lesson> page;
        if (keyword != null && !keyword.trim().isEmpty()) {
            page = lessonRepository.searchLessons(keyword.trim(), pageable);
        } else {
            page = lessonRepository.findAll(pageable);
        }
        return page.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonDto> getByClassId(Long classId) {
        return lessonRepository.findByClassesIdOrderByOrderNumberAsc(classId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private LessonDto mapToDto(Lesson entity) {
        return LessonDto.builder()
                .id(entity.getId())
                .classId(entity.getClasses().getId())
                .classCode(entity.getClasses().getCode())
                .className(entity.getClasses().getName())
                .name(entity.getName())
                .orderNumber(entity.getOrderNumber())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private User getCurrentUser(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    private void checkTeacherClassPermission(User currentUser, Long classId) {
        if (classId == null) return;

        if (currentUser != null) {
            Long userId = currentUser.getId();
            Long staffId = staffRepository.findByUserId(userId).map(Staff::getId).orElse(null);

            boolean isAssigned = scheduleAssignmentRepository.isTeacherAssignedToClass(classId, userId, staffId);
            boolean isSubstituting = teachingSubstitutionRepository.isTeacherSubstitutingForClass(classId, userId, staffId);

            if (!isAssigned && !isSubstituting) {
                throw new OperationNotPermittedException("Bạn không được phân công giảng dạy lớp học này nên không có quyền thao tác bài học!");
            }
        }
    }
}
