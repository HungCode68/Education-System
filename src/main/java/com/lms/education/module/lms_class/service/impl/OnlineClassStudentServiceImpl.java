package com.lms.education.module.lms_class.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms_class.dto.OnlineClassStudentDto;
import com.lms.education.module.lms_class.entity.OnlineClass;
import com.lms.education.module.lms_class.entity.OnlineClassStudent;
import com.lms.education.module.lms_class.repository.OnlineClassRepository;
import com.lms.education.module.lms_class.repository.OnlineClassStudentRepository;
import com.lms.education.module.lms_class.service.OnlineClassStudentService;
import com.lms.education.module.academic.entity.ClassStudent; // Entity quan hệ lớp-hs vật lý
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.academic.repository.ClassStudentRepository; // Cần repo này
import com.lms.education.module.user.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineClassStudentServiceImpl implements OnlineClassStudentService {

    private final OnlineClassStudentRepository onlineClassStudentRepository;
    private final OnlineClassRepository onlineClassRepository;
    private final StudentRepository studentRepository;

    // Inject Repository của module Student để lấy danh sách gốc
    private final ClassStudentRepository classStudentRepository;

    @Override
    @Transactional
    public OnlineClassStudentDto addStudentManual(String onlineClassId, String studentId) {
        // Validate tồn tại
        OnlineClass onlineClass = onlineClassRepository.findById(onlineClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học không tồn tại"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Học sinh không tồn tại"));

        // Check trùng
        Optional<OnlineClassStudent> existing = onlineClassStudentRepository
                .findByOnlineClassIdAndStudentId(onlineClassId, studentId);

        if (existing.isPresent()) {
            OnlineClassStudent entity = existing.get();
            if (entity.getStatus() == OnlineClassStudent.StudentStatus.active) {
                throw new DuplicateResourceException("Học sinh này đã có trong lớp.");
            } else {
                // Nếu đã bị xóa trước đó -> Kích hoạt lại
                entity.setStatus(OnlineClassStudent.StudentStatus.active);
                entity.setEnrollmentSource(OnlineClassStudent.EnrollmentSource.manual);
                return mapToDto(onlineClassStudentRepository.save(entity));
            }
        }

        // Tạo mới
        OnlineClassStudent newEntity = OnlineClassStudent.builder()
                .onlineClass(onlineClass)
                .student(student)
                .enrollmentSource(OnlineClassStudent.EnrollmentSource.manual)
                .status(OnlineClassStudent.StudentStatus.active)
                .enrolledDate(LocalDate.now())
                .build();

        return mapToDto(onlineClassStudentRepository.save(newEntity));
    }

    @Override
    @Transactional
    public void removeStudent(String onlineClassId, String studentId) {
        OnlineClassStudent entity = onlineClassStudentRepository
                .findByOnlineClassIdAndStudentId(onlineClassId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Học sinh không tồn tại trong lớp này"));

        // Soft delete
        entity.setStatus(OnlineClassStudent.StudentStatus.removed);
        onlineClassStudentRepository.save(entity);
    }

    @Override
    public List<OnlineClassStudentDto> getStudentsByClass(String onlineClassId, OnlineClassStudent.StudentStatus status) {
        // Nếu status null -> Lấy hết, ngược lại lọc theo status
        List<OnlineClassStudent> list;
        if (status == null) {
            list = onlineClassStudentRepository.findAllByOnlineClassId(onlineClassId);
        } else {
            list = onlineClassStudentRepository.findAllByOnlineClassIdAndStatus(onlineClassId, status);
        }

        return list.stream().map(this::mapToDto).toList();
    }

    @Override
    public List<OnlineClassStudentDto> getClassesByStudent(String studentId) {
        return onlineClassStudentRepository.findAllByStudentId(studentId).stream()
                .map(this::mapToDto)
                .toList();
    }

    // =================================================================
    // CORE LOGIC: AUTO SYNC
    // =================================================================
    @Override
    @Transactional
    public void syncStudentsFromPhysicalClass(String onlineClassId) {
        log.info("Bắt đầu đồng bộ học sinh cho Online Class ID: {}", onlineClassId);

        OnlineClass onlineClass = onlineClassRepository.findById(onlineClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học không tồn tại"));

        // Lấy ID lớp vật lý từ OnlineClass -> TeachingAssignment
        String physicalClassId = onlineClass.getTeachingAssignment().getPhysicalClass().getId();

        // Lấy danh sách học sinh ĐANG HỌC (Active) ở lớp vật lý
        List<ClassStudent> physicalStudents = classStudentRepository
                .findAllByPhysicalClassIdAndStatus(physicalClassId, ClassStudent.StudentStatus.studying); // Status bên module Student

        int countAdded = 0;

        // Duyệt và thêm vào lớp Online
        for (ClassStudent ps : physicalStudents) {
            Student student = ps.getStudent();

            // Kiểm tra xem đã có trong lớp Online chưa
            if (!onlineClassStudentRepository.existsByOnlineClassIdAndStudentId(onlineClassId, student.getId())) {

                // Nếu chưa có -> Thêm mới (Source = SYSTEM)
                OnlineClassStudent newStudent = OnlineClassStudent.builder()
                        .onlineClass(onlineClass)
                        .student(student)
                        .enrollmentSource(OnlineClassStudent.EnrollmentSource.system)
                        .status(OnlineClassStudent.StudentStatus.active)
                        .enrolledDate(LocalDate.now())
                        .build();

                onlineClassStudentRepository.save(newStudent);
                countAdded++;
            }
            // Nếu đã có rồi thì bỏ qua (kể cả manual hay system), không ghi đè để giữ nguyên lịch sử
        }

        log.info("Đã đồng bộ xong. Thêm mới {} học sinh vào lớp Online.", countAdded);
    }

    // --- MAPPER ---
    private OnlineClassStudentDto mapToDto(OnlineClassStudent entity) {
        return OnlineClassStudentDto.builder()
                .id(entity.getId())
                .onlineClassId(entity.getOnlineClass().getId())
                .onlineClassName(entity.getOnlineClass().getName())

                .studentId(entity.getStudent().getId())
                .studentCode(entity.getStudent().getStudentCode())
                .studentName(entity.getStudent().getFullName())

                .enrollmentSource(entity.getEnrollmentSource())
                .enrolledDate(entity.getEnrolledDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
