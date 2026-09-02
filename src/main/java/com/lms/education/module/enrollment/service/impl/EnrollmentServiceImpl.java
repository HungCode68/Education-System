package com.lms.education.module.enrollment.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.enrollment.dto.BulkEnrollmentDto;
import com.lms.education.module.enrollment.dto.EnrollmentDto;
import com.lms.education.module.enrollment.entity.Enrollment;
import com.lms.education.module.enrollment.repository.EnrollmentRepository;
import com.lms.education.module.enrollment.service.EnrollmentService;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassesRepository classesRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional
    public EnrollmentDto create(EnrollmentDto dto) {
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên với ID: " + dto.getStudentId()));

        Classes classes = classesRepository.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + dto.getClassId()));

        if (enrollmentRepository.existsByStudentIdAndClassesId(dto.getStudentId(), dto.getClassId())) {
            throw new DuplicateResourceException("Học viên này đã đăng ký lớp học này rồi!");
        }

        String status = dto.getStatus();
        if (status == null || status.trim().isEmpty()) {
            status = "ACTIVE";
        } else {
            status = status.trim().toUpperCase();
        }

        if (isCountingToCapacity(status)) {
            checkAndIncrementCapacity(classes);
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .classes(classes)
                .enrollmentDate(dto.getEnrollmentDate())
                .status(status)
                .note(dto.getNote())
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Đăng ký thành công học viên: {} vào lớp: {} với trạng thái: {}",
                student.getFullName(), classes.getName(), status);

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public EnrollmentDto update(Long id, EnrollmentDto dto) {
        Enrollment existing = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin đăng ký với ID: " + id));

        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên với ID: " + dto.getStudentId()));

        Classes newClass = classesRepository.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + dto.getClassId()));

        // Check duplicate only if student/class changes
        if ((!existing.getStudent().getId().equals(dto.getStudentId()) || !existing.getClasses().getId().equals(dto.getClassId()))
                && enrollmentRepository.existsByStudentIdAndClassesId(dto.getStudentId(), dto.getClassId())) {
            throw new DuplicateResourceException("Học viên này đã đăng ký lớp học này rồi!");
        }

        String newStatus = dto.getStatus();
        if (newStatus == null || newStatus.trim().isEmpty()) {
            newStatus = "ACTIVE";
        } else {
            newStatus = newStatus.trim().toUpperCase();
        }

        String oldStatus = existing.getStatus();
        Classes oldClass = existing.getClasses();

        // Handle class and status change logic
        if (oldClass.getId().equals(newClass.getId())) {
            boolean wasCounting = isCountingToCapacity(oldStatus);
            boolean isCounting = isCountingToCapacity(newStatus);

            if (wasCounting && !isCounting) {
                decrementCapacity(oldClass);
            } else if (!wasCounting && isCounting) {
                checkAndIncrementCapacity(oldClass);
            }
        } else {
            // Class changed
            if (isCountingToCapacity(oldStatus)) {
                decrementCapacity(oldClass);
            }
            if (isCountingToCapacity(newStatus)) {
                checkAndIncrementCapacity(newClass);
            }
        }

        existing.setStudent(student);
        existing.setClasses(newClass);
        existing.setEnrollmentDate(dto.getEnrollmentDate());
        existing.setStatus(newStatus);
        existing.setNote(dto.getNote());

        Enrollment updated = enrollmentRepository.save(existing);
        log.info("Cập nhật đăng ký ID: {} thành công", id);

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Enrollment existing = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin đăng ký với ID: " + id));

        if (isCountingToCapacity(existing.getStatus())) {
            decrementCapacity(existing.getClasses());
        }

        enrollmentRepository.delete(existing);
        log.info("Đã xóa đăng ký học viên ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentDto getById(Long id) {
        Enrollment existing = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin đăng ký với ID: " + id));
        return mapToDto(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getAll(String keyword, Pageable pageable) {
        Page<Enrollment> page;
        if (keyword != null && !keyword.trim().isEmpty()) {
            page = enrollmentRepository.searchEnrollments(keyword.trim(), pageable);
        } else {
            page = enrollmentRepository.findAll(pageable);
        }
        return page.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDto> getByStudentId(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDto> getByClassId(Long classId) {
        return enrollmentRepository.findByClassesId(classId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private boolean isCountingToCapacity(String status) {
        return "ACTIVE".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status);
    }

    private void checkAndIncrementCapacity(Classes classes) {
        int current = classes.getCurrentStudents() != null ? classes.getCurrentStudents() : 0;
        int max = classes.getMaxStudents() != null ? classes.getMaxStudents() : 20;

        if (current >= max) {
            throw new OperationNotPermittedException("Lớp học \"" + classes.getName() + "\" đã đạt sĩ số tối đa (" + max + ")!");
        }

        classes.setCurrentStudents(current + 1);
        classesRepository.save(classes);
    }

    private void decrementCapacity(Classes classes) {
        int current = classes.getCurrentStudents() != null ? classes.getCurrentStudents() : 0;
        if (current > 0) {
            classes.setCurrentStudents(current - 1);
            classesRepository.save(classes);
        }
    }

    private EnrollmentDto mapToDto(Enrollment entity) {
        return EnrollmentDto.builder()
                .id(entity.getId())
                .studentId(entity.getStudent().getId())
                .studentName(entity.getStudent().getFullName())
                .studentCode(entity.getStudent().getStudentCode())
                .classId(entity.getClasses().getId())
                .classCode(entity.getClasses().getCode())
                .className(entity.getClasses().getName())
                .enrollmentDate(entity.getEnrollmentDate())
                .status(entity.getStatus())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> enrollBulk(BulkEnrollmentDto dto) {
        Classes classes = classesRepository.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + dto.getClassId()));

        String status = dto.getStatus();
        if (status == null || status.trim().isEmpty()) {
            status = "ACTIVE";
        } else {
            status = status.trim().toUpperCase();
        }

        LocalDate enrollmentDate = dto.getEnrollmentDate();
        if (enrollmentDate == null) {
            enrollmentDate = LocalDate.now();
        }

        List<Long> successes = new ArrayList<>();
        Map<Long, String> failures = new HashMap<>();

        for (Long studentId : dto.getStudentIds()) {
            try {
                Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên với ID: " + studentId));

                if (enrollmentRepository.existsByStudentIdAndClassesId(studentId, classes.getId())) {
                    throw new DuplicateResourceException("Học viên này đã đăng ký lớp học này rồi!");
                }

                if (isCountingToCapacity(status)) {
                    // Check capacity dynamically per student
                    int current = classes.getCurrentStudents() != null ? classes.getCurrentStudents() : 0;
                    int max = classes.getMaxStudents() != null ? classes.getMaxStudents() : 20;

                    if (current >= max) {
                        throw new OperationNotPermittedException("Lớp học đã đạt sĩ số tối đa (" + max + ")!");
                    }

                    // Increment
                    classes.setCurrentStudents(current + 1);
                    classesRepository.save(classes);
                }

                Enrollment enrollment = Enrollment.builder()
                        .student(student)
                        .classes(classes)
                        .enrollmentDate(enrollmentDate)
                        .status(status)
                        .note(dto.getNote())
                        .build();

                enrollmentRepository.save(enrollment);
                successes.add(studentId);

                log.info("Đăng ký thành công học viên ID: {} vào lớp: {} hàng loạt", studentId, classes.getName());

            } catch (Exception e) {
                failures.put(studentId, e.getMessage());
                log.warn("Đăng ký học viên ID: {} vào lớp: {} hàng loạt thất bại: {}", studentId, classes.getName(), e.getMessage());
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("totalAttempted", dto.getStudentIds().size());
        report.put("successCount", successes.size());
        report.put("failureCount", failures.size());
        report.put("successes", successes);
        report.put("failures", failures);

        return report;
    }

    @Override
    @Transactional
    public void handleStudentStatusCascade(Long studentId, String globalStatus) {
        if (!"DROPPED".equalsIgnoreCase(globalStatus) && !"RESERVED".equalsIgnoreCase(globalStatus)) {
            return;
        }

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        int cascadeCount = 0;

        for (Enrollment e : enrollments) {
            if ("ACTIVE".equalsIgnoreCase(e.getStatus()) || "PENDING".equalsIgnoreCase(e.getStatus())) {
                decrementCapacity(e.getClasses());
                e.setStatus("DROPPED");
                
                String currentNote = e.getNote() != null ? e.getNote() + " | " : "";
                e.setNote(currentNote + "Hệ thống tự động hủy lớp do học viên chuyển trạng thái thành: " + globalStatus);
                
                enrollmentRepository.save(e);
                cascadeCount++;
                log.info("Đã tự động cập nhật trạng thái DROPPED cho học viên ID {} tại lớp {}", studentId, e.getClasses().getName());
            }
        }
        
        if (cascadeCount > 0) {
            log.info("Đã đồng bộ hủy trạng thái (DROPPED) cho {} lớp học của học viên ID {}", cascadeCount, studentId);
        }
    }
}
