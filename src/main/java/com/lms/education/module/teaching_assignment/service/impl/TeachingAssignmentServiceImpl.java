package com.lms.education.module.teaching_assignment.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms_class.repository.OnlineClassRepository;
import com.lms.education.module.lms_class.entity.OnlineClass;
import com.lms.education.module.lms_class.service.OnlineClassStudentService;
import com.lms.education.module.teaching_assignment.dto.TeachingAssignmentDto;
import com.lms.education.module.academic.entity.*;
import com.lms.education.module.academic.repository.*;
import com.lms.education.module.teaching_assignment.entity.TeachingAssignment;
import com.lms.education.module.teaching_assignment.entity.TeachingAssignmentHistory;
import com.lms.education.module.teaching_assignment.entity.TeachingSubstitution;
import com.lms.education.module.teaching_assignment.repository.TeachingAssignmentRepository;
import com.lms.education.module.teaching_assignment.repository.TeachingSubstitutionRepository;
import com.lms.education.module.teaching_assignment.service.TeachingAssignmentHistoryService;
import com.lms.education.module.teaching_assignment.service.TeachingAssignmentService;
import com.lms.education.module.user.entity.Teacher;
import com.lms.education.module.user.repository.TeacherRepository;
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
public class TeachingAssignmentServiceImpl implements TeachingAssignmentService {

    private final TeachingAssignmentRepository assignmentRepository;
    private final PhysicalClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final SemesterRepository semesterRepository;
    private final OnlineClassRepository onlineClassRepository;
    private final OnlineClassStudentService onlineClassStudentService;
    private final GradeSubjectRepository gradeSubjectRepository;
    private final TeachingSubstitutionRepository substitutionRepository;
    private final TeachingAssignmentHistoryService historyService;

    @Override
    @Transactional
    public TeachingAssignmentDto assignTeacher(TeachingAssignmentDto dto) {
        log.info("Phân công: GV {} dạy môn {} cho lớp {}", dto.getTeacherId(), dto.getSubjectId(), dto.getPhysicalClassId());

        // VALIDATE DỮ LIỆU ĐẦU VÀO (Tồn tại hay không?)
        PhysicalClass physicalClass = classRepository.findById(dto.getPhysicalClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học không tồn tại"));

        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Môn học không tồn tại"));

        Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên không tồn tại"));

        Semester semester = semesterRepository.findById(dto.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Học kỳ không tồn tại"));

        // Validate logic phụ: Học kỳ này có thuộc Năm học được gửi lên không?
        if (!semester.getSchoolYear().getId().equals(dto.getSchoolYearId())) {
            throw new IllegalArgumentException("Học kỳ không thuộc năm học đã chọn");
        }

        // Validate logic phụ 2: Năm học của lớp có khớp với năm học của Học kỳ không?
        // (Đây là logic chặt chẽ để tránh dữ liệu rác)
        if (!physicalClass.getSchoolYear().getId().equals(semester.getSchoolYear().getId())) {
            throw new IllegalArgumentException("Năm học của lớp và Học kỳ không khớp nhau.");
        }

        SchoolYear schoolYear = schoolYearRepository.findById(dto.getSchoolYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Năm học không tồn tại"));

        // LOGIC "GÁC CỔNG" (GUARD CLAUSES)

        // Kiểm tra xem Lớp này + Môn này + Kỳ này ĐÃ CÓ AI DẠY CHƯA?
        Optional<TeachingAssignment> existingAssignment = assignmentRepository
                .findByPhysicalClassIdAndSubjectIdAndSemesterIdAndStatus(
                        dto.getPhysicalClassId(),
                        dto.getSubjectId(),
                        dto.getSemesterId(),
                        TeachingAssignment.AssignmentStatus.active
                );

        TeachingAssignment entity;
        Teacher oldTeacher = null;      // Biến lưu giáo viên cũ để ghi log
        TeachingAssignmentHistory.ActionType actionType;

        if (existingAssignment.isPresent()) {
            // Đã có người dạy -> Cập nhật người mới (Thay thế)
            entity = existingAssignment.get();
            oldTeacher = entity.getTeacher();
            log.info("Thay đổi giáo viên môn {} lớp {}: {} -> {}",
                    subject.getName(), physicalClass.getName(), entity.getTeacher().getFullName(), teacher.getFullName());

            // Nếu người mới trùng người cũ -> Báo lỗi hoặc return luôn
            if (entity.getTeacher().getId().equals(teacher.getId())) {
                throw new DuplicateResourceException("Giáo viên " + teacher.getFullName() + " đã được phân công dạy lớp này rồi!");
            }

            entity.setTeacher(teacher); // Đổi sang giáo viên mới
            actionType = TeachingAssignmentHistory.ActionType.REPLACED;
            // Giữ nguyên ngày tạo, cập nhật ngày update tự động
        } else {
            // Case B: Chưa có ai dạy -> Tạo mới hoàn toàn
            entity = TeachingAssignment.builder()
                    .physicalClass(physicalClass)
                    .subject(subject)
                    .teacher(teacher)
                    .schoolYear(schoolYear)
                    .semester(semester)
                    .status(TeachingAssignment.AssignmentStatus.active)
                    .onlineClassId(dto.getOnlineClassId()) // Optional
                    .build();
            actionType = TeachingAssignmentHistory.ActionType.ASSIGNED;
        }

        TeachingAssignment savedAssignment = assignmentRepository.save(entity);
        createOnlineClassIfNeeded(savedAssignment);

        historyService.log(
                savedAssignment,
                oldTeacher,           // Người cũ (null nếu tạo mới)
                teacher,              // Người mới
                actionType,           // ASSIGNED hoặc REPLACED
                "Phân công giáo viên giảng dạy", // Reason
                "Admin"               // ChangedBy
        );

        // LƯU XUỐNG DB
        return mapToDto(savedAssignment);
    }

    // HÀM LOGIC TẠO LỚP LMS
    private void createOnlineClassIfNeeded(TeachingAssignment assignment) {
        // Kiểm tra cấu hình trong bảng GradeSubject
        boolean isLmsAllowed = checkGradeSubjectConfig(assignment.getSubject(), assignment.getPhysicalClass());

        if (!isLmsAllowed) {
            log.info("Môn {} của Lớp {} không được cấu hình bật LMS -> Bỏ qua tạo lớp online.",
                    assignment.getSubject().getName(), assignment.getPhysicalClass().getName());
            return;
        }

        // Kiểm tra xem đã tồn tại lớp Online cho phân công này chưa?
        OnlineClass onlineClass;
        Optional<OnlineClass> existingClass = onlineClassRepository.findByTeachingAssignmentId(assignment.getId());

        if (existingClass.isPresent()) {
            onlineClass = existingClass.get();
        } else {
            // Tạo mới
            String onlineClassName = assignment.getSubject().getName() + " - " + assignment.getPhysicalClass().getName();
            OnlineClass newClass = OnlineClass.builder()
                    .name(onlineClassName)
                    .teachingAssignment(assignment)
                    .status("active")
                    .build();
            onlineClass = onlineClassRepository.save(newClass);
            log.info("Đã tự động tạo Online Class: {}", onlineClassName);
        }

        // GỌI HÀM ĐỒNG BỘ HỌC SINH NGAY TẠI ĐÂY
        // Dù là lớp mới tạo hay lớp cũ, ta đều nên chạy sync 1 lần
        // để đảm bảo nếu lớp vật lý có học sinh mới thì lớp Online cũng cập nhật theo.
        onlineClassStudentService.syncStudentsFromPhysicalClass(onlineClass.getId());
    }

    // --- HÀM CHECK CẤU HÌNH THẬT SỰ ---
    private boolean checkGradeSubjectConfig(Subject subject, PhysicalClass physicalClass) {
        // Lấy GradeId từ lớp vật lý
        String gradeId = physicalClass.getGrade().getId();
        String subjectId = subject.getId();

        // Tìm trong bảng GradeSubject xem có cấu hình cho cặp (Grade, Subject) này không
        return gradeSubjectRepository.findByGradeIdAndSubjectId(gradeId, subjectId)
                .map(GradeSubject::getIsLmsEnabled) // Lấy giá trị cờ isLmsEnabled
                .orElse(false); // Nếu không tìm thấy cấu hình -> Mặc định là FALSE (An toàn)
    }

    @Override
    @Transactional
    public void unassignTeacher(String assignmentId) {
        // Xóa phân công
        TeachingAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công ID: " + assignmentId));

        Teacher oldTeacher = assignment.getTeacher();
        // CÁCH 1: CHẶN XÓA (An toàn nhất)
        // Cần Inject thêm TeachingSubstitutionRepository để check
        /*
        if (substitutionRepository.existsByOriginalAssignmentId(assignmentId)) {
            throw new OperationNotPermittedException("Không thể xóa phân công này vì đang có lịch sử dạy thay liên quan. Vui lòng gỡ bỏ dữ liệu dạy thay trước.");
        }
        */

        // CÁCH 2: SOFT DELETE (Khuyên dùng cho v1)
        // Thay vì xóa bay màu, ta chỉ set status về inactive
        assignment.setStatus(TeachingAssignment.AssignmentStatus.inactive);
        assignmentRepository.save(assignment);

        historyService.log(
                assignment,
                oldTeacher,
                null, // Không có người mới
                TeachingAssignmentHistory.ActionType.UNASSIGNED,
                "Hủy phân công giảng dạy",
                "Admin"
        );

        log.info("Đã chuyển phân công ID {} sang trạng thái inactive", assignmentId);
    }

    @Override
    public List<TeachingAssignmentDto> getAssignmentsByClass(String classId, String semesterId) {
        // Hàm này dùng để hiển thị danh sách GV bộ môn của lớp
        return assignmentRepository.findAllByClassAndSemester(classId, semesterId).stream()
                .map(this::mapToDto)
                .toList();
    }

    // Hàm này hỗ trợ tính năng "Gợi ý" hoặc kiểm tra tải
    public long countTeacherWorkload(String teacherId, String semesterId) {
        return assignmentRepository.countByTeacherIdAndSemesterIdAndStatus(
                teacherId, semesterId, TeachingAssignment.AssignmentStatus.active
        );
    }

    // --- MAPPER ---
    private TeachingAssignmentDto mapToDto(TeachingAssignment entity) {
        TeachingAssignmentDto dto = TeachingAssignmentDto.builder()
                .id(entity.getId())
                // Class Info
                .physicalClassId(entity.getPhysicalClass().getId())
                .physicalClassName(entity.getPhysicalClass().getName())
                // Subject Info
                .subjectId(entity.getSubject().getId())
                .subjectName(entity.getSubject().getName())
                // Teacher Info
                .teacherId(entity.getTeacher().getId())
                .teacherName(entity.getTeacher().getFullName())
                .teacherCode(entity.getTeacher().getTeacherCode())
                // Time Info
                .schoolYearId(entity.getSchoolYear().getId())
                .schoolYearName(entity.getSchoolYear().getName())
                .semesterId(entity.getSemester().getId())
                .semesterName(entity.getSemester().getName())
                // Extra
                .onlineClassId(entity.getOnlineClassId())
                .status(entity.getStatus())
                .assignedDate(entity.getAssignedDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // [LOGIC MỚI] KIỂM TRA DẠY THAY
        // Kiểm tra xem HÔM NAY (LocalDate.now()) phân công này có ai dạy thay không?
        Optional<TeachingSubstitution> activeSub = substitutionRepository.findActiveSubstitution(
                entity.getId(),
                LocalDate.now()
        );

        if (activeSub.isPresent()) {
            TeachingSubstitution sub = activeSub.get();
            // Gán thông tin dạy thay vào DTO
            dto.setSubstituted(true);
            dto.setSubTeacherId(sub.getSubTeacher().getId());
            dto.setSubTeacherName(sub.getSubTeacher().getFullName());
            dto.setSubStartDate(sub.getStartDate());
            dto.setSubEndDate(sub.getEndDate());
        } else {
            dto.setSubstituted(false);
        }

        return dto;

    }
}
