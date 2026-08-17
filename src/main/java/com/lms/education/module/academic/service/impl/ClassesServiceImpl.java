package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.ClassesDto;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.entity.Term;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.academic.repository.ScheduleCancellationRepository;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.academic.repository.TermRepository;
import com.lms.education.module.academic.service.ClassesService;
import com.lms.education.module.academic.entity.ClassSchedule;
import com.lms.education.module.academic.entity.ScheduleCancellation;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.UserRepository;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassesServiceImpl implements ClassesService {

    private final ClassesRepository classesRepository;
    private final CourseRepository courseRepository;
    private final TermRepository termRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ScheduleCancellationRepository scheduleCancellationRepository;

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
                
        LocalDate estimatedEndDate = calculateExactEndDate(classes);
        if (estimatedEndDate != null) {
            classes.setEndDate(estimatedEndDate);
        }

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
        // Do not set endDate from DTO. Recalculate it.
        LocalDate preciseEndDate = calculateExactEndDate(classes);
        if (preciseEndDate != null) {
            classes.setEndDate(preciseEndDate);
        }
        
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

    @Override
    @Transactional(readOnly = true)
    public List<ClassesDto> getMyClasses() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return List.of();
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return List.of();
        }

        Long userId = user.getId();
        Long staffId = staffRepository.findByUserId(userId).map(Staff::getId).orElse(null);

        Set<Long> classIds = new HashSet<>();
        if (staffId != null || userId != null) {
            List<Long> scheduleClassIds = scheduleAssignmentRepository.findClassIdsByTeacher(userId, staffId);
            classIds.addAll(scheduleClassIds);

            if (staffId != null) {
                teachingAssignmentRepository.findByTeacherId(staffId).forEach(ta -> {
                    if (ta.getClasses() != null) {
                        classIds.add(ta.getClasses().getId());
                    }
                });
            }
        }

        if (classIds.isEmpty()) {
            return List.of();
        }

        return classesRepository.findAllById(classIds).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void recalculateEndDate(Long classId) {
        Classes classes = classesRepository.findById(classId).orElse(null);
        if (classes == null) return;
        
        LocalDate newEndDate = calculateExactEndDate(classes);
        if (newEndDate != null) {
            classes.setEndDate(newEndDate);
            classesRepository.save(classes);
        }
    }
    
    @Override
    @Transactional
    public void recalculateAllActiveClasses() {
        List<Classes> activeClasses = classesRepository.findAll();
        for (Classes classes : activeClasses) {
            if (!"CLOSED".equals(classes.getStatus()) && !"COMPLETED".equals(classes.getStatus())) {
                LocalDate newEndDate = calculateExactEndDate(classes);
                if (newEndDate != null) {
                    classes.setEndDate(newEndDate);
                    classesRepository.save(classes);
                }
            }
        }
    }
    
    private LocalDate calculateExactEndDate(Classes classes) {
        if (classes.getStartDate() == null || classes.getCourse() == null || classes.getCourse().getTotalSessions() == null) {
            return classes.getEndDate(); // Fallback
        }
        
        int totalSessions = classes.getCourse().getTotalSessions();
        int sessionsPerWeek = classes.getCourse().getSessionsPerWeek() != null ? classes.getCourse().getSessionsPerWeek() : 0;
        
        List<ClassSchedule> schedules = classes.getId() != null ? classScheduleRepository.findByClassesId(classes.getId()) : Collections.emptyList();
        if (schedules.isEmpty()) {
            // Rough estimation
            if (sessionsPerWeek > 0) {
                int weeks = (int) Math.ceil((double) totalSessions / sessionsPerWeek);
                return classes.getStartDate().plusWeeks(weeks);
            }
            return classes.getEndDate();
        }
        
        // Exact calculation
        Set<Integer> validDaysOfWeek = schedules.stream()
                .map(ClassSchedule::getDayOfWeek)
                .collect(Collectors.toSet());
                
        List<ScheduleCancellation> cancellations = scheduleCancellationRepository.findByClassIdOrCenterWide(classes.getId());
        
        int countedSessions = 0;
        LocalDate currentDate = classes.getStartDate();
        LocalDate limitDate = classes.getStartDate().plusYears(5); // safety limit
        
        while (countedSessions < totalSessions && currentDate.isBefore(limitDate)) {
            int currentDayOfWeek = currentDate.getDayOfWeek().getValue() + 1; // 1=Mon->2, 7=Sun->8
            
            if (validDaysOfWeek.contains(currentDayOfWeek)) {
                LocalDate finalCurrentDate = currentDate;
                boolean isCancelled = cancellations.stream().anyMatch(c -> 
                    !finalCurrentDate.isBefore(c.getStartDate()) && !finalCurrentDate.isAfter(c.getEndDate())
                );
                
                if (!isCancelled) {
                    countedSessions++;
                }
            }
            
            if (countedSessions < totalSessions) {
                currentDate = currentDate.plusDays(1);
            }
        }
        
        return currentDate;
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
