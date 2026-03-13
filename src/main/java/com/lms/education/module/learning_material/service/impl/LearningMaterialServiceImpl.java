package com.lms.education.module.learning_material.service.impl;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.learning_material.dto.LearningMaterialDto;
import com.lms.education.module.learning_material.entity.LearningMaterial;
import com.lms.education.module.learning_material.repository.LearningMaterialRepository;
import com.lms.education.module.learning_material.service.LearningMaterialService;
import com.lms.education.module.learning_material.service.MinioStorageService;
import com.lms.education.module.lms_class.entity.OnlineClass;
import com.lms.education.module.lms_class.entity.OnlineClassStudent;
import com.lms.education.module.lms_class.repository.OnlineClassRepository;
import com.lms.education.module.lms_class.repository.OnlineClassStudentRepository;
import com.lms.education.module.user.entity.Teacher;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningMaterialServiceImpl implements LearningMaterialService {

    private final LearningMaterialRepository materialRepository;
    private final OnlineClassRepository classRepository;
    private final UserRepository userRepository;
    private final MinioStorageService minioStorageService;
    private final OnlineClassStudentRepository onlineClassStudentRepository;
    private final com.lms.education.module.user.repository.StudentRepository studentRepository;
    private final com.lms.education.module.user.repository.TeacherRepository teacherRepository;

    @Override
    @Transactional
    public LearningMaterialDto uploadMaterial(MultipartFile file, LearningMaterialDto dto, String uploaderId) {
        checkTeacherPermissionForClass(dto.getOnlineClassId(), uploaderId);
        OnlineClass onlineClass = classRepository.findById(dto.getOnlineClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học không tồn tại"));

        User uploader = userRepository.findByEmail(uploaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        // Upload file lên MinIO
        String objectName = minioStorageService.uploadFile(file);

        // Lưu thông tin vào Database
        LearningMaterial material = LearningMaterial.builder()
                .onlineClass(onlineClass)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .fileType(dto.getFileType())
                .filePath(objectName) // Lưu Object Name của MinIO
                .fileSize(file.getSize())
                .fileName(file.getOriginalFilename())
                .status(dto.getStatus() != null ? dto.getStatus() : LearningMaterial.MaterialStatus.unpublished)
                .uploadedBy(uploader)
                .build();

        return mapToDto(materialRepository.save(material));
    }

    @Override
    @Transactional
    public LearningMaterialDto addLinkMaterial(LearningMaterialDto dto, String uploaderId) {
        checkTeacherPermissionForClass(dto.getOnlineClassId(), uploaderId);
        OnlineClass onlineClass = classRepository.findById(dto.getOnlineClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học không tồn tại"));

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        // Link thì không có MinIO, filePath chính là URL youtube/drive
        LearningMaterial material = LearningMaterial.builder()
                .onlineClass(onlineClass)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .fileType(LearningMaterial.FileType.link)
                .filePath(dto.getFilePath()) // URL do Frontend gửi lên
                .status(dto.getStatus() != null ? dto.getStatus() : LearningMaterial.MaterialStatus.unpublished)
                .uploadedBy(uploader)
                .build();

        return mapToDto(materialRepository.save(material));
    }

    @Override
    public List<LearningMaterialDto> getMaterialsForTeacher(String classId, String username) {
        checkTeacherPermissionForClass(classId, username);
        return materialRepository.findAllByOnlineClassId(classId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public List<LearningMaterialDto> getMaterialsForStudent(String classId, String username) {
        checkStudentPermissionForClass(classId, username);
        return materialRepository.findAllByOnlineClassIdAndStatus(classId, LearningMaterial.MaterialStatus.published).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteMaterial(String materialId, String username) {
        LearningMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài liệu không tồn tại"));

        checkTeacherPermissionForClass(material.getOnlineClass().getId(), username);

        // Nếu là file thật (không phải link) thì gọi MinIO để xóa file vật lý
        if (material.getFileType() != LearningMaterial.FileType.link && material.getFilePath() != null) {
            minioStorageService.deleteFile(material.getFilePath());
        }

        materialRepository.delete(material);
        log.info("Đã xóa tài liệu ID: {}", materialId);
    }

    @Override
    @Transactional
    public void changeStatus(String materialId, LearningMaterial.MaterialStatus status, String username) {
        LearningMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài liệu không tồn tại"));
        checkTeacherPermissionForClass(material.getOnlineClass().getId(), username);
        material.setStatus(status);
        materialRepository.save(material);
    }

    @Override
    public String getMaterialDownloadUrl(String materialId, String username) {
        LearningMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài liệu không tồn tại"));

        // Kiểm tra xem User này có quyền truy cập vào Lớp chứa tài liệu này không
        String classId = material.getOnlineClass().getId();
        User currentReqUser = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        // Logic check: Nếu là role STUDENT thì check xem có trong lớp không, nếu là TEACHER thì check xem có dạy không.
        if (currentReqUser.getRole().equals("STUDENT")) {
            checkStudentPermissionForClass(classId, username);
            // Học sinh thì chỉ được tải file ĐÃ PUBLISHED
            if (material.getStatus() != LearningMaterial.MaterialStatus.published) {
                throw new AccessDeniedException("Tài liệu này chưa được xuất bản");
            }
        } else if (currentReqUser.getRole().equals("SUBJECT_TEACHER")) {
            checkTeacherPermissionForClass(classId, username);
        } else {
            throw new AccessDeniedException("Bạn không có quyền truy cập");
        }

        if (material.getFileType() == LearningMaterial.FileType.link) {
            return material.getFilePath();
        }
        String presignedUrl = minioStorageService.getFileUrl(material.getFilePath());
        if (presignedUrl == null) {
            throw new RuntimeException("Không thể lấy được đường dẫn tải file");
        }
        return presignedUrl;
    }

    private void checkTeacherPermissionForClass(String classId, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));

        // TỪ EMAIL (USERNAME), TÌM RA HỒ SƠ GIÁO VIÊN
        Teacher teacher = teacherRepository.findByUser_Email(username)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản của bạn chưa được liên kết với hồ sơ Giáo viên"));

        // Dùng teacher.getId() để check xem có dạy lớp này không
        List<OnlineClass> myClasses = classRepository.findAllClassesForTeacher(teacher.getId(), LocalDate.now());
        boolean isAssigned = myClasses.stream()
                .anyMatch(onlineClass -> onlineClass.getId().equals(classId));

        if (!isAssigned) {
            throw new AccessDeniedException("Bạn không phụ trách lớp học này, không thể xem hoặc tải tài liệu.");
        }
    }

    private void checkStudentPermissionForClass(String classId, String username) {
        com.lms.education.module.user.entity.Student student = studentRepository.findByUser_Email(username)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản của bạn chưa được liên kết với hồ sơ Học sinh"));

        // Tìm bản ghi ghi danh của học sinh trong lớp online này
        Optional<OnlineClassStudent> enrollment = onlineClassStudentRepository
                .findByOnlineClassIdAndStudentId(classId, student.getId());

        // Kiểm tra: Phải tồn tại TRONG LỚP và TRẠNG THÁI đang là ACTIVE (chưa bị xóa)
        boolean isEnrolledAndActive = enrollment.isPresent() &&
                enrollment.get().getStatus() == OnlineClassStudent.StudentStatus.active;

        if (!isEnrolledAndActive) {
            throw new AccessDeniedException("Bạn không phải là thành viên của lớp này hoặc đã bị xóa khỏi lớp, không thể xem/tải tài liệu.");
        }
    }

    // --- MAPPER ---
    private LearningMaterialDto mapToDto(LearningMaterial entity) {
        LearningMaterialDto dto = LearningMaterialDto.builder()
                .id(entity.getId())
                .onlineClassId(entity.getOnlineClass().getId())
                .onlineClassName(entity.getOnlineClass().getName())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .fileType(entity.getFileType())
                .fileSize(entity.getFileSize())
                .fileName(entity.getFileName())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        if (entity.getUploadedBy() != null) {
            dto.setUploadedById(entity.getUploadedBy().getId());
            dto.setUploadedByName(entity.getUploadedBy().getEmail());
        }

        // --- XỬ LÝ ĐƯỜNG DẪN FILE ---
        // TỐI ƯU HÓA TRẢ DỮ LIỆU
        if (entity.getFileType() == LearningMaterial.FileType.link) {
            // Nếu là link ngoài (Youtube/Drive) thì Frontend vẫn cần để hiện luôn
            dto.setFilePath(entity.getFilePath());
        } else {
            // Nếu là file upload, giấu cái tên UUID xấu xí đi và KHÔNG gọi MinIO xin link nữa
            dto.setFilePath(null);
        }

        return dto;
    }
}
