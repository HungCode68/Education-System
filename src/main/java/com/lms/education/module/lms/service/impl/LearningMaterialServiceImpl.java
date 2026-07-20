package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms.dto.LearningMaterialDto;
import com.lms.education.module.lms.entity.LearningMaterial;
import com.lms.education.module.lms.entity.Lesson;
import com.lms.education.module.lms.repository.LearningMaterialRepository;
import com.lms.education.module.lms.repository.LessonRepository;
import com.lms.education.module.lms.service.LearningMaterialService;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.UserRepository;
import com.lms.education.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningMaterialServiceImpl implements LearningMaterialService {

    private final LearningMaterialRepository learningMaterialRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final TeachingSubstitutionRepository teachingSubstitutionRepository;
    private final MinioStorageService minioStorageService;

    @Override
    @Transactional
    public LearningMaterialDto createWithFile(Long lessonId, String title, String materialType, Integer displayOrder, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new OperationNotPermittedException("File upload không được để trống!");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học với ID: " + lessonId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        // Kiểm tra phân công giảng dạy cho Lớp học chứa bài học này
        checkTeacherClassPermission(currentUser, auth, lesson.getClasses().getId());

        // Upload file lên MinIO
        String objectName = minioStorageService.uploadFile(file);

        boolean isOfficial = isAcademicDepartmentUser(currentUser, auth);
        String formattedType = materialType != null ? materialType.trim().toUpperCase() : "DOCUMENT";
        boolean isRagEnabled = false;
        String indexingStatus = "NOT_INDEXED";

        LearningMaterial material = LearningMaterial.builder()
                .lesson(lesson)
                .title(title != null ? title.trim() : file.getOriginalFilename())
                .materialType(formattedType)
                .sourceType("MINIO")
                .resourceUrl(objectName)
                .fileSize(file.getSize())
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .isOfficial(isOfficial)
                .isRagEnabled(isRagEnabled)
                .indexingStatus(indexingStatus)
                .uploadedBy(currentUser)
                .build();

        LearningMaterial saved = learningMaterialRepository.save(material);
        log.info("Đã tạo mới tài liệu tệp: {} (ID: {}) cho bài học: {}, isOfficial={}, isRagEnabled={}",
                saved.getTitle(), saved.getId(), lesson.getName(), isOfficial, isRagEnabled);

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public LearningMaterialDto createExternalLink(LearningMaterialDto dto) {
        if (dto.getResourceUrl() == null || dto.getResourceUrl().trim().isEmpty()) {
            throw new OperationNotPermittedException("Đường dẫn liên kết (resourceUrl) không được để trống!");
        }

        Lesson lesson = lessonRepository.findById(dto.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học với ID: " + dto.getLessonId()));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        // Kiểm tra phân công giảng dạy cho Lớp học chứa bài học này
        checkTeacherClassPermission(currentUser, auth, lesson.getClasses().getId());

        boolean isOfficial = isAcademicDepartmentUser(currentUser, auth);

        LearningMaterial material = LearningMaterial.builder()
                .lesson(lesson)
                .title(dto.getTitle().trim())
                .materialType(dto.getMaterialType() != null ? dto.getMaterialType().trim().toUpperCase() : "EXTERNAL_LINK")
                .sourceType("EXTERNAL")
                .resourceUrl(dto.getResourceUrl().trim())
                .fileSize(null)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .isOfficial(isOfficial)
                .isRagEnabled(false)
                .indexingStatus("NOT_INDEXED")
                .uploadedBy(currentUser)
                .build();

        LearningMaterial saved = learningMaterialRepository.save(material);
        log.info("Đã tạo mới tài liệu link ngoài: {} (ID: {}) cho bài học: {}, isOfficial={}",
                saved.getTitle(), saved.getId(), lesson.getName(), isOfficial);

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public LearningMaterialDto update(Long id, LearningMaterialDto dto, MultipartFile file) {
        LearningMaterial existing = learningMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu học tập với ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        // Kiểm tra phân công giảng dạy cho Lớp học chứa bài học này
        checkTeacherClassPermission(currentUser, auth, existing.getLesson().getClasses().getId());

        if (dto.getLessonId() != null) {
            Lesson lesson = lessonRepository.findById(dto.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học với ID: " + dto.getLessonId()));
            checkTeacherClassPermission(currentUser, auth, lesson.getClasses().getId());
            existing.setLesson(lesson);
        }

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            existing.setTitle(dto.getTitle().trim());
        }

        if (dto.getMaterialType() != null && !dto.getMaterialType().trim().isEmpty()) {
            existing.setMaterialType(dto.getMaterialType().trim().toUpperCase());
        }

        if (dto.getDisplayOrder() != null) {
            existing.setDisplayOrder(dto.getDisplayOrder());
        }

        if (dto.getIsOfficial() != null) {
            boolean isAcademic = isAcademicDepartmentUser(currentUser, auth);
            if (isAcademic) {
                existing.setIsOfficial(dto.getIsOfficial());
            } else if (!existing.getIsOfficial().equals(dto.getIsOfficial())) {
                throw new OperationNotPermittedException("Chỉ Bộ phận Đào tạo mới có quyền phê duyệt hoặc thay đổi trạng thái Tài liệu Chính thống (isOfficial)!");
            }
        }

        if (dto.getIsRagEnabled() != null) {
            boolean isAcademic = isAcademicDepartmentUser(currentUser, auth);
            if (isAcademic) {
                existing.setIsRagEnabled(dto.getIsRagEnabled());
                if (Boolean.TRUE.equals(dto.getIsRagEnabled()) && "NOT_INDEXED".equals(existing.getIndexingStatus())) {
                    existing.setIndexingStatus("PENDING");
                } else if (Boolean.FALSE.equals(dto.getIsRagEnabled())) {
                    existing.setIndexingStatus("NOT_INDEXED");
                }
            } else if (!existing.getIsRagEnabled().equals(dto.getIsRagEnabled())) {
                throw new OperationNotPermittedException("Chỉ Bộ phận Đào tạo mới có quyền bật/tắt tính năng RAG AI cho tài liệu (isRagEnabled)!");
            }
        }

        // Trường hợp người dùng tải file mới thay thế
        if (file != null && !file.isEmpty()) {
            if ("MINIO".equalsIgnoreCase(existing.getSourceType())) {
                minioStorageService.deleteFile(existing.getResourceUrl());
            }

            String newObjectName = minioStorageService.uploadFile(file);
            existing.setSourceType("MINIO");
            existing.setResourceUrl(newObjectName);
            existing.setFileSize(file.getSize());
        } else if (dto.getResourceUrl() != null && !dto.getResourceUrl().trim().isEmpty()) {
            if ("MINIO".equalsIgnoreCase(existing.getSourceType())) {
                minioStorageService.deleteFile(existing.getResourceUrl());
            }
            existing.setSourceType("EXTERNAL");
            existing.setResourceUrl(dto.getResourceUrl().trim());
            existing.setFileSize(null);
        }

        LearningMaterial updated = learningMaterialRepository.save(existing);
        log.info("Đã cập nhật tài liệu học tập ID: {}", id);

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LearningMaterial existing = learningMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu học tập với ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUser(auth);

        // Kiểm tra phân công giảng dạy cho Lớp học chứa bài học này
        checkTeacherClassPermission(currentUser, auth, existing.getLesson().getClasses().getId());

        if ("MINIO".equalsIgnoreCase(existing.getSourceType())) {
            minioStorageService.deleteFile(existing.getResourceUrl());
        }

        learningMaterialRepository.delete(existing);
        log.info("Đã xóa tài liệu học tập ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public LearningMaterialDto getById(Long id) {
        LearningMaterial existing = learningMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu học tập với ID: " + id));
        return mapToDto(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningMaterialDto> getByLessonId(Long lessonId) {
        return learningMaterialRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningMaterialDto> getByClassId(Long classId) {
        return learningMaterialRepository.findByLessonClassesIdOrderByDisplayOrderAsc(classId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningMaterialDto> getAll(String keyword, Pageable pageable) {
        Page<LearningMaterial> page;
        if (keyword != null && !keyword.trim().isEmpty()) {
            page = learningMaterialRepository.searchMaterials(keyword.trim(), pageable);
        } else {
            page = learningMaterialRepository.findAll(pageable);
        }
        return page.map(this::mapToDto);
    }

    private LearningMaterialDto mapToDto(LearningMaterial entity) {
        String downloadUrl = entity.getResourceUrl();
        if ("MINIO".equalsIgnoreCase(entity.getSourceType())) {
            downloadUrl = minioStorageService.getFileUrl(entity.getResourceUrl());
        }

        return LearningMaterialDto.builder()
                .id(entity.getId())
                .lessonId(entity.getLesson().getId())
                .lessonName(entity.getLesson().getName())
                .classId(entity.getLesson().getClasses().getId())
                .className(entity.getLesson().getClasses().getName())
                .title(entity.getTitle())
                .materialType(entity.getMaterialType())
                .sourceType(entity.getSourceType())
                .resourceUrl(entity.getResourceUrl())
                .downloadUrl(downloadUrl)
                .fileSize(entity.getFileSize())
                .displayOrder(entity.getDisplayOrder())
                .isOfficial(entity.getIsOfficial())
                .isRagEnabled(entity.getIsRagEnabled())
                .indexingStatus(entity.getIndexingStatus())
                .uploadedById(entity.getUploadedBy() != null ? entity.getUploadedBy().getId() : null)
                .uploadedByName(entity.getUploadedBy() != null ? entity.getUploadedBy().getFullName() : null)
                .uploadedByEmail(entity.getUploadedBy() != null ? entity.getUploadedBy().getEmail() : null)
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

    private boolean isAcademicDepartmentUser(User user, Authentication auth) {
        if (auth != null && auth.getAuthorities() != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                String roleName = authority.getAuthority().toUpperCase();
                if (roleName.contains("ADMIN") || roleName.contains("MANAGER") || roleName.contains("ACADEMIC") || roleName.contains("TRAINING")) {
                    return true;
                }
            }
        }
        if (user != null && user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role.getId() != null && role.getId() == 8L) {
                    return true;
                }
                String roleName = role.getName().toUpperCase();
                if (roleName.contains("ADMIN") || roleName.contains("MANAGER") || roleName.contains("ACADEMIC") || roleName.contains("TRAINING")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkTeacherClassPermission(User currentUser, Authentication auth, Long classId) {
        if (classId == null) return;

        // Kiểm tra người dùng hiện tại có được phân công giảng dạy hoặc dạy thay lớp này hay không
        if (currentUser != null) {
            Long userId = currentUser.getId();
            Long staffId = staffRepository.findByUserId(userId).map(Staff::getId).orElse(null);

            boolean isAssigned = scheduleAssignmentRepository.isTeacherAssignedToClass(classId, userId, staffId);
            boolean isSubstituting = teachingSubstitutionRepository.isTeacherSubstitutingForClass(classId, userId, staffId);

            if (!isAssigned && !isSubstituting) {
                throw new OperationNotPermittedException("Bạn không được phân công giảng dạy lớp học này nên không có quyền thao tác tài liệu!");
            }
        }
    }
}
