package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms.dto.AssignmentDto;
import com.lms.education.module.lms.entity.Assignment;
import com.lms.education.module.lms.entity.Lesson;
import com.lms.education.module.lms.repository.AssignmentRepository;
import com.lms.education.module.lms.repository.LessonRepository;
import com.lms.education.module.lms.service.AssignmentService;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.UserRepository;
import com.lms.education.module.lms.repository.SubmissionRepository;
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
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final TeachingSubstitutionRepository teachingSubstitutionRepository;

    @Override
    @Transactional
    public AssignmentDto create(AssignmentDto dto) {
        Lesson lesson = lessonRepository.findById(dto.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học với ID: " + dto.getLessonId()));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        // Kiểm tra phân công giảng dạy cho Lớp học chứa bài học này
        checkTeacherClassPermission(currentUser, lesson.getClasses().getId());

        // Validations nghiệp vụ 
        if (dto.getDueDate() != null && dto.getDueDate().isBefore(java.time.LocalDateTime.now())) {
            throw new OperationNotPermittedException("Hạn nộp bài (dueDate) phải ở trong tương lai!");
        }

        if (dto.getTimeLimitMinutes() != null && dto.getTimeLimitMinutes() < 0) {
            throw new OperationNotPermittedException("Thời gian làm bài (timeLimitMinutes) không được là số âm!");
        }

        if (dto.getMaxAttempts() != null && dto.getMaxAttempts() < 1) {
            throw new OperationNotPermittedException("Số lần làm bài tối đa (maxAttempts) phải từ 1 trở lên!");
        }

        Assignment assignment = Assignment.builder()
                .lesson(lesson)
                .title(dto.getTitle().trim())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .assignmentType(dto.getAssignmentType() != null && !dto.getAssignmentType().trim().isEmpty() ? dto.getAssignmentType().trim().toUpperCase() : "HOMEWORK")
                .timeLimitMinutes(dto.getTimeLimitMinutes() != null ? dto.getTimeLimitMinutes() : 0)
                .maxAttempts(dto.getMaxAttempts() != null ? dto.getMaxAttempts() : 1)
                .status(dto.getStatus() != null && !dto.getStatus().trim().isEmpty() ? dto.getStatus().trim().toUpperCase() : "PUBLISHED")
                .build();

        Assignment saved = assignmentRepository.save(assignment);
        log.info("Đã tạo mới bài tập: {} (ID: {}) cho bài học: {}", saved.getTitle(), saved.getId(), lesson.getName());

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public AssignmentDto update(Long id, AssignmentDto dto) {
        Assignment existing = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        checkTeacherClassPermission(currentUser, existing.getLesson().getClasses().getId());

        if (dto.getLessonId() != null) {
            Lesson lesson = lessonRepository.findById(dto.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học với ID: " + dto.getLessonId()));
            checkTeacherClassPermission(currentUser, lesson.getClasses().getId());
            existing.setLesson(lesson);
        }

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            existing.setTitle(dto.getTitle().trim());
        }

        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }

        if (dto.getDueDate() != null) {
            if (dto.getDueDate().isBefore(java.time.LocalDateTime.now())) {
                throw new OperationNotPermittedException("Hạn nộp bài (dueDate) phải ở trong tương lai!");
            }
            existing.setDueDate(dto.getDueDate());
        }

        if (dto.getAssignmentType() != null && !dto.getAssignmentType().trim().isEmpty()) {
            existing.setAssignmentType(dto.getAssignmentType().trim().toUpperCase());
        }

        if (dto.getTimeLimitMinutes() != null) {
            if (dto.getTimeLimitMinutes() < 0) {
                throw new OperationNotPermittedException("Thời gian làm bài (timeLimitMinutes) không được là số âm!");
            }
            existing.setTimeLimitMinutes(dto.getTimeLimitMinutes());
        }

        if (dto.getMaxAttempts() != null) {
            if (dto.getMaxAttempts() < 1) {
                throw new OperationNotPermittedException("Số lần làm bài tối đa (maxAttempts) phải từ 1 trở lên!");
            }
            existing.setMaxAttempts(dto.getMaxAttempts());
        }

        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            String newStatus = dto.getStatus().trim().toUpperCase();
            if (("UNPUBLISHED".equalsIgnoreCase(newStatus) || "DRAFT".equalsIgnoreCase(newStatus))
                    && ("PUBLISHED".equalsIgnoreCase(existing.getStatus()) || "CLOSED".equalsIgnoreCase(existing.getStatus()))) {
                boolean hasSubmissions = submissionRepository.existsByAssignmentId(id);
                if (hasSubmissions) {
                    throw new OperationNotPermittedException("Bài tập này đã có học viên mở bài làm hoặc nộp bài, không thể chuyển về bản nháp (UNPUBLISHED)!");
                }
            }
            existing.setStatus(newStatus);
        }

        Assignment updated = assignmentRepository.save(existing);
        log.info("Đã cập nhật bài tập ID: {}", id);

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Assignment existing = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        checkTeacherClassPermission(currentUser, existing.getLesson().getClasses().getId());

        boolean hasSubmissions = submissionRepository.existsByAssignmentId(id);
        if (hasSubmissions) {
            throw new OperationNotPermittedException("Bài tập này đã có dữ liệu làm bài/nộp bài của học viên, không thể xóa!");
        }

        assignmentRepository.delete(existing);
        log.info("Đã xóa bài tập ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentDto getById(Long id) {
        Assignment existing = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        if (!canViewDraftAssignment(currentUser, existing.getLesson().getClasses().getId())
                && ("UNPUBLISHED".equalsIgnoreCase(existing.getStatus()) || "DRAFT".equalsIgnoreCase(existing.getStatus()))) {
            throw new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + id);
        }

        return mapToDto(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentDto> getByLessonId(Long lessonId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        Long classId = lesson != null ? lesson.getClasses().getId() : null;

        boolean canViewDraft = canViewDraftAssignment(currentUser, classId);

        return assignmentRepository.findByLessonIdOrderByDueDateAsc(lessonId).stream()
                .filter(a -> canViewDraft || (!"UNPUBLISHED".equalsIgnoreCase(a.getStatus()) && !"DRAFT".equalsIgnoreCase(a.getStatus())))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentDto> getByClassId(Long classId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        boolean canViewDraft = canViewDraftAssignment(currentUser, classId);

        return assignmentRepository.findByLessonClassesIdOrderByDueDateAsc(classId).stream()
                .filter(a -> canViewDraft || (!"UNPUBLISHED".equalsIgnoreCase(a.getStatus()) && !"DRAFT".equalsIgnoreCase(a.getStatus())))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentDto> getAll(String keyword, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        boolean isStaffOrTeacher = isStaffOrTeacherUser(currentUser);

        Page<Assignment> page;
        if (keyword != null && !keyword.trim().isEmpty()) {
            page = assignmentRepository.searchAssignments(keyword.trim(), pageable);
        } else {
            page = assignmentRepository.findAll(pageable);
        }

        if (!isStaffOrTeacher) {
            // Lọc ra các bài tập chưa phát hành đối với học viên
            List<AssignmentDto> filtered = page.getContent().stream()
                    .filter(a -> !"UNPUBLISHED".equalsIgnoreCase(a.getStatus()) && !"DRAFT".equalsIgnoreCase(a.getStatus()))
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
            return new org.springframework.data.domain.PageImpl<>(filtered, pageable, page.getTotalElements());
        }

        return page.map(this::mapToDto);
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void autoCloseExpiredAssignments() {
        int count = assignmentRepository.closeExpiredAssignments(java.time.LocalDateTime.now());
        if (count > 0) {
            log.info("Đã tự động chuyển {} bài tập hết hạn sang trạng thái CLOSED", count);
        }
    }

    private AssignmentDto mapToDto(Assignment entity) {
        String status = entity.getStatus();
        if (entity.getDueDate() != null && java.time.LocalDateTime.now().isAfter(entity.getDueDate()) && "PUBLISHED".equalsIgnoreCase(status)) {
            status = "CLOSED";
        }

        return AssignmentDto.builder()
                .id(entity.getId())
                .lessonId(entity.getLesson().getId())
                .lessonName(entity.getLesson().getName())
                .classId(entity.getLesson().getClasses().getId())
                .classCode(entity.getLesson().getClasses().getCode())
                .className(entity.getLesson().getClasses().getName())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .dueDate(entity.getDueDate())
                .assignmentType(entity.getAssignmentType())
                .timeLimitMinutes(entity.getTimeLimitMinutes())
                .maxAttempts(entity.getMaxAttempts())
                .status(status)
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
                throw new OperationNotPermittedException("Bạn không được phân công giảng dạy lớp học này nên không có quyền thao tác bài tập!");
            }
        }
    }

    private boolean canViewDraftAssignment(User currentUser, Long classId) {
        if (currentUser == null) return false;
        if (isStaffOrTeacherUser(currentUser)) return true;
        if (classId == null) return false;

        Long userId = currentUser.getId();
        Long staffId = staffRepository.findByUserId(userId).map(Staff::getId).orElse(null);

        boolean isAssigned = scheduleAssignmentRepository.isTeacherAssignedToClass(classId, userId, staffId);
        boolean isSubstituting = teachingSubstitutionRepository.isTeacherSubstitutingForClass(classId, userId, staffId);

        return isAssigned || isSubstituting;
    }

    private boolean isStaffOrTeacherUser(User currentUser) {
        if (currentUser != null && currentUser.getRoles() != null) {
            return currentUser.getRoles().stream().anyMatch(role -> {
                String name = role.getName().toUpperCase();
                return name.contains("ADMIN") || name.contains("MANAGER") || name.contains("ACADEMIC") || name.contains("TEACHER") || name.contains("TRAINING");
            });
        }
        return false;
    }
}
