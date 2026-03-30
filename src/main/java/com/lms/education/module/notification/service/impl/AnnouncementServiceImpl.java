package com.lms.education.module.notification.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.notification.dto.AnnouncementDto;
import com.lms.education.module.notification.entity.Announcement;
import com.lms.education.module.notification.repository.AnnouncementRepository;
import com.lms.education.module.notification.service.AnnouncementService;
import com.lms.education.module.lms_class.entity.OnlineClass;
import com.lms.education.module.academic.entity.PhysicalClass;
import com.lms.education.module.lms_class.repository.OnlineClassRepository;
import com.lms.education.module.academic.repository.PhysicalClassRepository;
import com.lms.education.module.learning_material.service.MinioStorageService;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.TeacherRepository;
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
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final MinioStorageService minioStorageService;
    private final UserRepository userRepository;
    private final PhysicalClassRepository physicalClassRepository;
    private final OnlineClassRepository onlineClassRepository;
    private final TeacherRepository teacherRepository;

    @Override
    @Transactional
    public AnnouncementDto createAnnouncement(AnnouncementDto dto, MultipartFile file, String userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại!"));

        Announcement announcement = Announcement.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .scope(dto.getScope())
                .createdBy(creator)
                .publishedAt(LocalDateTime.now())
                .build();

        // Kiểm tra Scope và gán vào đúng loại Lớp học
        if (dto.getScope() == Announcement.AnnouncementScope.physical_class) {
            PhysicalClass physicalClass = physicalClassRepository.findById(dto.getPhysicalClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Lớp học Offline!"));
            announcement.setPhysicalClass(physicalClass);
        } else if (dto.getScope() == Announcement.AnnouncementScope.online_class) {
            OnlineClass onlineClass = onlineClassRepository.findById(dto.getOnlineClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Lớp học Online!"));
            announcement.setOnlineClass(onlineClass);
        }

        // Xử lý Upload File lên MinIO (Nếu có)
        if (file != null && !file.isEmpty()) {
            String objectName = minioStorageService.uploadFile(file);
            announcement.setAttachmentPath(objectName);
            log.info("Đã đính kèm file {} vào thông báo mới", objectName);
        }

        Announcement savedAnnouncement = announcementRepository.save(announcement);
        log.info("User {} đã đăng thông báo '{}' cho {}", creator.getEmail(), savedAnnouncement.getTitle(), savedAnnouncement.getScope());

        return mapToDto(savedAnnouncement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementDto> getAnnouncementsByPhysicalClass(String classId, Pageable pageable) {
        return announcementRepository.findByPhysicalClassIdOrderByPublishedAtDesc(classId, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementDto> getAnnouncementsByOnlineClass(String classId, Pageable pageable) {
        return announcementRepository.findByOnlineClassIdOrderByPublishedAtDesc(classId, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public AnnouncementDto updateAnnouncement(String announcementId, AnnouncementDto dto, MultipartFile file, String userId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo!"));

        // Kiểm tra quyền
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại!"));

        boolean isAdmin = currentUser.getRole().getCode().equalsIgnoreCase("HOMEROOM_TEACHER");
        if (!isAdmin && !announcement.getCreatedBy().getId().equals(userId)) {
            throw new OperationNotPermittedException("Bạn không có quyền sửa thông báo của người khác!");
        }

        // Cập nhật thông tin text
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());

        // Xử lý File đính kèm (Nếu có upload file mới)
        if (file != null && !file.isEmpty()) {
            // Xóa file cũ trên MinIO (nếu trước đó thông báo này có file)
            if (announcement.getAttachmentPath() != null) {
                minioStorageService.deleteFile(announcement.getAttachmentPath());
                log.info("Đã xóa file đính kèm cũ: {}", announcement.getAttachmentPath());
            }

            // Upload file mới và lưu đường dẫn mới
            String objectName = minioStorageService.uploadFile(file);
            announcement.setAttachmentPath(objectName);
            log.info("Đã đính kèm file mới {} vào thông báo", objectName);
        }

        Announcement updatedAnnouncement = announcementRepository.save(announcement);
        log.info("User {} đã cập nhật thông báo ID: {}", currentUser.getEmail(), announcementId);

        return mapToDto(updatedAnnouncement);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(String announcementId, String userId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo!"));

        // Kiểm tra quyền xóa (Chỉ người đăng hoặc Admin mới được xóa)
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại!"));

        boolean isAdmin = currentUser.getRole().getCode().equalsIgnoreCase("ADMIN");
        if (!isAdmin && !announcement.getCreatedBy().getId().equals(userId)) {
            throw new OperationNotPermittedException("Bạn không có quyền xóa thông báo của người khác!");
        }

        //  Xóa file vật lý trên MinIO (Nếu có file đính kèm)
        if (announcement.getAttachmentPath() != null) {
            minioStorageService.deleteFile(announcement.getAttachmentPath());
        }

        // Xóa data trong DB
        announcementRepository.delete(announcement);
        log.info("Đã xóa thông báo ID: {}", announcementId);
    }

    // HÀM HELPER

    private AnnouncementDto mapToDto(Announcement entity) {
        AnnouncementDto dto = AnnouncementDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .scope(entity.getScope())
                .publishedAt(entity.getPublishedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // Lấy thông tin Lớp học
        if (entity.getPhysicalClass() != null) {
            dto.setPhysicalClassId(entity.getPhysicalClass().getId());
            dto.setPhysicalClassName(entity.getPhysicalClass().getName());
        }
        if (entity.getOnlineClass() != null) {
            dto.setOnlineClassId(entity.getOnlineClass().getId());
            dto.setOnlineClassName(entity.getOnlineClass().getName());
        }

        // Lấy thông tin Người đăng
        if (entity.getCreatedBy() != null) {
            String userId = entity.getCreatedBy().getId();

            dto.setCreatedById(userId);
            dto.setCreatedByEmail(entity.getCreatedBy().getEmail());

            teacherRepository.findByUserId(userId).ifPresentOrElse(
                    teacher -> {
                        // Nếu tìm thấy hồ sơ giáo viên, lấy tên thật
                        dto.setCreatedByName(teacher.getFullName());
                    },
                    () -> {
                        // Nếu không tìm thấy (Ví dụ người đăng là Admin không có hồ sơ Giáo viên),
                        dto.setCreatedByName("Quản trị viên");
                    }
            );
        }

        // Tự động sinh link tải từ MinIO nếu có file đính kèm
        if (entity.getAttachmentPath() != null && !entity.getAttachmentPath().isEmpty()) {
            dto.setAttachmentPath(entity.getAttachmentPath());
            dto.setAttachmentUrl(minioStorageService.getFileUrl(entity.getAttachmentPath()));
        }

        return dto;
    }
}
