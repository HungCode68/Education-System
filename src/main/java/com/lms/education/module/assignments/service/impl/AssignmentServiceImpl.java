package com.lms.education.module.assignments.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.learning_material.service.MinioStorageService;
import com.lms.education.module.lms_class.entity.OnlineClass;
import com.lms.education.module.lms_class.repository.OnlineClassRepository;
import com.lms.education.module.assignments.dto.AssignmentDto;
import com.lms.education.module.assignments.entity.Assignment;
import com.lms.education.module.assignments.repository.AssignmentRepository;
import com.lms.education.module.assignments.service.AssignmentService;
import com.lms.education.module.lms_class.repository.OnlineClassStudentRepository;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final OnlineClassRepository onlineClassRepository;
    private final UserRepository userRepository;
    private final OnlineClassStudentRepository onlineClassStudentRepository;
    private final MinioStorageService minioStorageService;
    private final StudentRepository studentRepository;

    @Override
    @Transactional
    public AssignmentDto create(AssignmentDto dto, MultipartFile file, String userId) {
        // Validate thời gian
        validateAssignmentTime(dto.getStartTime(), dto.getDueTime());

        // Phân quyền: Kiểm tra xem User này có phải là Giáo viên của Lớp này không
        validateUserInClass(userId, dto.getOnlineClassId(), true);

        // Kiểm tra Lớp học và Người tạo có tồn tại không
        OnlineClass onlineClass = onlineClassRepository.findById(dto.getOnlineClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Lớp học với ID: " + dto.getOnlineClassId()));

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người tạo"));

        // XỬ LÝ UPLOAD FILE LÊN MINIO
        String attachmentPath = null;
        if (file != null && !file.isEmpty()) {
            attachmentPath = minioStorageService.uploadFile(file);
        }

        // Build Entity để lưu
        Assignment assignment = Assignment.builder()
                .onlineClass(onlineClass)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .attachmentPath(attachmentPath)
                .assignmentType(dto.getAssignmentType())
                .startTime(dto.getStartTime())
                .dueTime(dto.getDueTime())
                .durationMinutes(dto.getDurationMinutes())
                .maxScore(dto.getMaxScore())
                .allowLateSubmission(dto.getAllowLateSubmission())
                .maxAttempts(dto.getMaxAttempts())
                .shuffleQuestions(dto.getShuffleQuestions())
                .viewAnswers(dto.getViewAnswers())
                .status(dto.getStatus() != null ? dto.getStatus() : Assignment.AssignmentStatus.unpublished)
                .createdBy(creator)
                .build();

        Assignment savedAssignment = assignmentRepository.save(assignment);
        log.info("Giáo viên {} đã tạo thành công bài tập: {}", creator.getEmail(), savedAssignment.getTitle());

        return mapToDto(savedAssignment);
    }

    @Override
    @Transactional
    public AssignmentDto update(String id, AssignmentDto dto, MultipartFile file, String userId) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập"));

        // Phân quyền: Kiểm tra người dùng có phải giáo viên của lớp này không
        validateUserInClass(userId, assignment.getOnlineClass().getId(), true);

        // Chỉ người tạo ra bài tập mới được phép sửa
        if (!assignment.getCreatedBy().getId().equals(userId)) {
            throw new OperationNotPermittedException("Bạn không có quyền chỉnh sửa bài tập của giáo viên khác!");
        }

        validateAssignmentTime(dto.getStartTime(), dto.getDueTime());

        // XỬ LÝ CẬP NHẬT FILE
        if (file != null && !file.isEmpty()) {
            // Có upload file mới -> Xóa file cũ trên MinIO (nếu có)
            if (assignment.getAttachmentPath() != null) {
                minioStorageService.deleteFile(assignment.getAttachmentPath());
            }
            // Upload file mới và gán lại đường dẫn
            String newAttachmentPath = minioStorageService.uploadFile(file);
            assignment.setAttachmentPath(newAttachmentPath);
        }
        // NẾU file == null (Tức là không chọn file mới) -> Hệ thống không làm gì cả,
        // tự động GIỮ NGUYÊN đường dẫn file cũ đang có sẵn trong biến 'assignment'.

        // CẬP NHẬT CÁC THÔNG TIN TEXT TỪ JSON DTO
        if (dto.getOnlineClassId() != null && !dto.getOnlineClassId().equals(assignment.getOnlineClass().getId())) {
            OnlineClass newClass = onlineClassRepository.findById(dto.getOnlineClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Lớp học"));
            assignment.setOnlineClass(newClass);
        }

        assignment.setTitle(dto.getTitle());
        assignment.setDescription(dto.getDescription());
        assignment.setAssignmentType(dto.getAssignmentType());
        assignment.setStartTime(dto.getStartTime());
        assignment.setDueTime(dto.getDueTime());
        assignment.setDurationMinutes(dto.getDurationMinutes());
        assignment.setMaxScore(dto.getMaxScore());
        assignment.setAllowLateSubmission(dto.getAllowLateSubmission());
        assignment.setMaxAttempts(dto.getMaxAttempts());
        assignment.setShuffleQuestions(dto.getShuffleQuestions());
        assignment.setViewAnswers(dto.getViewAnswers());

        if (dto.getStatus() != null) {
            assignment.setStatus(dto.getStatus());
        }

        return mapToDto(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public void delete(String id, String userId) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập"));

        // Phân quyền: Kiểm tra người dùng có phải giáo viên của lớp này không
        validateUserInClass(userId, assignment.getOnlineClass().getId(), true);

        if (!assignment.getCreatedBy().getId().equals(userId)) {
            throw new OperationNotPermittedException("Bạn không có quyền xóa bài tập này!");
        }

        // XÓA FILE TRÊN MINIO TRƯỚC KHI XÓA DATA TRONG DATABASE
        if (assignment.getAttachmentPath() != null) {
            minioStorageService.deleteFile(assignment.getAttachmentPath());
        }

        assignmentRepository.delete(assignment);
        log.info("Bài tập {} đã bị xóa bởi user {}", id, userId);
    }

    @Override
    public AssignmentDto getById(String id, String userId) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập"));
        // Phân quyền: Kiểm tra xem User (Giáo viên hoặc Học sinh) có thuộc lớp này không
        validateUserInClass(userId, assignment.getOnlineClass().getId(), false);

        return mapToDto(assignment);
    }

    @Override
    public Page<AssignmentDto> getAssignmentsByClass(String classId, Pageable pageable, String userId) {
        // Phân quyền: Chặn không cho người ngoài xem bài tập của lớp
        validateUserInClass(userId, classId, false);
        return assignmentRepository.findByOnlineClassId(classId, pageable).map(this::mapToDto);
    }

    @Override
    public Page<AssignmentDto> getAssignmentsByCreator(String userId, Pageable pageable) {
        return assignmentRepository.findByCreatedById(userId, pageable).map(this::mapToDto);
    }


    private void validateUserInClass(String userId, String classId, boolean mustBeTeacher) {
        OnlineClass onlineClass = onlineClassRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Lớp học với ID: " + classId));

        boolean isAssignedTeacher = false;
        if (onlineClass.getTeachingAssignment() != null
                && onlineClass.getTeachingAssignment().getTeacher() != null) {

            User teacherAccount = onlineClass.getTeachingAssignment().getTeacher().getUser();

            if (teacherAccount != null && teacherAccount.getId() != null) {
                String assignedTeacherUserId = teacherAccount.getId();

                if (assignedTeacherUserId.trim().equalsIgnoreCase(userId.trim())) {
                    isAssignedTeacher = true;
                } else {
                    log.warn("Auth Warning: Lớp này của Giáo viên mang Account ID = [{}], nhưng người đang thao tác là Account ID = [{}]",
                            assignedTeacherUserId, userId);
                }
            }
        }

        // Nếu API yêu cầu phải là Giáo viên (CRUD bài tập)
        if (mustBeTeacher) {
            if (!isAssignedTeacher) {
                throw new OperationNotPermittedException("Truy cập bị từ chối! Chỉ Giáo viên phụ trách lớp mới có quyền thực hiện thao tác này.");
            }
            return;
        }

        // Nếu API chỉ cần đọc (Học sinh/Giáo viên vào xem)
        if (isAssignedTeacher) {
            return;
        }

        // Kiểm tra quyền Học sinh
        Student studentProfile = studentRepository.findByUserId(userId).orElse(null);

        if (studentProfile != null) {
            String actualStudentId = studentProfile.getId();

            // Mang studentId thực sự đi kiểm tra xem có trong lớp không
            boolean isEnrolledStudent = onlineClassStudentRepository.existsByOnlineClassIdAndStudentId(classId, actualStudentId);

            if (isEnrolledStudent) {
                return; // Hợp lệ, cho phép Học sinh xem bài
            }
        }

        // Nếu rớt hết các vòng kiểm tra trên thì văng lỗi
        throw new OperationNotPermittedException("Bạn không có quyền truy cập! Bạn không phải là thành viên của lớp học này.");
    }

    // CÁC HÀM HỖ TRỢ
    private void validateAssignmentTime(LocalDateTime startTime, LocalDateTime dueTime) {
        if (startTime != null && dueTime != null) {
            if (dueTime.isBefore(startTime)) {
                throw new OperationNotPermittedException("Thời gian nộp bài (Hạn chót) không được diễn ra trước thời gian mở đề!");
            }
        }
    }

    private AssignmentDto mapToDto(Assignment assignment) {
        // TẠO PRE-SIGNED URL TỪ MINIO ĐỂ FRONTEND CÓ THỂ TẢI FILE
        String fileUrl = null;
        if (assignment.getAttachmentPath() != null) {
            fileUrl = minioStorageService.getFileUrl(assignment.getAttachmentPath());
        }
        return AssignmentDto.builder()
                .id(assignment.getId())
                .onlineClassId(assignment.getOnlineClass() != null ? assignment.getOnlineClass().getId() : null)
                .onlineClassName(assignment.getOnlineClass() != null ? assignment.getOnlineClass().getName() : null)
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .attachmentPath(assignment.getAttachmentPath())
                .attachmentUrl(fileUrl)
                .assignmentType(assignment.getAssignmentType())
                .startTime(assignment.getStartTime())
                .dueTime(assignment.getDueTime())
                .durationMinutes(assignment.getDurationMinutes())
                .maxScore(assignment.getMaxScore())
                .allowLateSubmission(assignment.getAllowLateSubmission())
                .maxAttempts(assignment.getMaxAttempts())
                .shuffleQuestions(assignment.getShuffleQuestions())
                .viewAnswers(assignment.getViewAnswers())
                .status(assignment.getStatus())
                .createdById(assignment.getCreatedBy() != null ? assignment.getCreatedBy().getId() : null)
                .createdByName(assignment.getCreatedBy() != null ? assignment.getCreatedBy().getEmail() : null) // Hiển thị tạm Email người tạo
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }
}
