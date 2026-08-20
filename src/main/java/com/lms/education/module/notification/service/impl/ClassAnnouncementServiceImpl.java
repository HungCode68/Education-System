package com.lms.education.module.notification.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.notification.dto.ClassAnnouncementDto;
import com.lms.education.module.notification.entity.ClassAnnouncement;
import com.lms.education.module.notification.repository.ClassAnnouncementRepository;
import com.lms.education.module.notification.service.ClassAnnouncementService;
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
import org.springframework.data.domain.PageRequest;
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
public class ClassAnnouncementServiceImpl implements ClassAnnouncementService {

    private final ClassAnnouncementRepository classAnnouncementRepository;
    private final ClassesRepository classesRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final TeachingSubstitutionRepository teachingSubstitutionRepository;
    private final MinioStorageService minioStorageService;

    @Override
    @Transactional
    public ClassAnnouncementDto createAnnouncement(ClassAnnouncementDto dto) {
        Classes classes = classesRepository.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + dto.getClassId()));

        User createdBy = resolveUser(dto.getCreatedById());
        if (createdBy == null) {
            throw new ResourceNotFoundException("Không xác định được thông tin người đăng thông báo!");
        }

        checkTeacherClassPermission(createdBy, classes.getId());

        boolean hasAtt = dto.getAttachmentUrl() != null && !dto.getAttachmentUrl().trim().isEmpty();

        ClassAnnouncement announcement = ClassAnnouncement.builder()
                .classes(classes)
                .createdBy(createdBy)
                .title(dto.getTitle())
                .content(dto.getContent())
                .hasAttachment(hasAtt)
                .attachmentUrl(dto.getAttachmentUrl())
                .isPinned(dto.getIsPinned() != null ? dto.getIsPinned() : false)
                .build();

        ClassAnnouncement saved = classAnnouncementRepository.save(announcement);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ClassAnnouncementDto createAnnouncementWithFile(Long classId, String title, String content, Boolean isPinned, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new OperationNotPermittedException("File upload không được để trống!");
        }

        Classes classes = classesRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        User createdBy = resolveUser(null);
        if (createdBy == null) {
            throw new ResourceNotFoundException("Bạn cần đăng nhập để thực hiện thao tác thông báo!");
        }

        checkTeacherClassPermission(createdBy, classes.getId());

        String objectName = minioStorageService.uploadFileKeepName(file);

        ClassAnnouncement announcement = ClassAnnouncement.builder()
                .classes(classes)
                .createdBy(createdBy)
                .title(title)
                .content(content)
                .hasAttachment(true)
                .attachmentUrl(objectName)
                .isPinned(isPinned != null ? isPinned : false)
                .build();

        ClassAnnouncement saved = classAnnouncementRepository.save(announcement);
        log.info("Đã tạo mới thông báo có đính kèm file: {} (ID: {}) cho lớp: {}", saved.getTitle(), saved.getId(), classes.getName());

        return toDto(saved);
    }

    @Override
    @Transactional
    public ClassAnnouncementDto updateAnnouncement(Long id, String title, String content, Boolean isPinned, Boolean removeAttachment, MultipartFile file) {
        ClassAnnouncement announcement = classAnnouncementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo với ID: " + id));

        User currentUser = resolveUser(null);
        checkTeacherClassPermission(currentUser, announcement.getClasses().getId());

        if (title != null && !title.trim().isEmpty()) {
            announcement.setTitle(title);
        }
        if (content != null && !content.trim().isEmpty()) {
            announcement.setContent(content);
        }
        if (isPinned != null) {
            announcement.setIsPinned(isPinned);
        }

        // Handle attachment
        if (Boolean.TRUE.equals(removeAttachment)) {
            if (announcement.getHasAttachment() && announcement.getAttachmentUrl() != null) {
                try {
                    String fileName = announcement.getAttachmentUrl();
                    if (fileName.contains("/")) {
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    }
                    minioStorageService.deleteFile(fileName);
                } catch (Exception e) {
                    log.error("Failed to delete old attachment for announcement ID {}: {}", id, e.getMessage());
                }
            }
            announcement.setAttachmentUrl(null);
            announcement.setHasAttachment(false);
        }

        if (file != null && !file.isEmpty()) {
            // Delete old if exists
            if (announcement.getHasAttachment() && announcement.getAttachmentUrl() != null) {
                try {
                    String oldName = announcement.getAttachmentUrl();
                    if (oldName.contains("/")) {
                        oldName = oldName.substring(oldName.lastIndexOf("/") + 1);
                    }
                    minioStorageService.deleteFile(oldName);
                } catch (Exception e) {
                    log.error("Failed to delete old attachment before replacing for announcement ID {}: {}", id, e.getMessage());
                }
            }
            String fileUrl = minioStorageService.uploadFileKeepName(file);
            announcement.setAttachmentUrl(fileUrl);
            announcement.setHasAttachment(true);
        }

        ClassAnnouncement updated = classAnnouncementRepository.save(announcement);
        return toDto(updated);
    }

    @Override
    @Transactional
    public ClassAnnouncementDto togglePin(Long id, boolean isPinned) {
        ClassAnnouncement announcement = classAnnouncementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo với ID: " + id));

        User currentUser = resolveUser(null);
        checkTeacherClassPermission(currentUser, announcement.getClasses().getId());

        announcement.setIsPinned(isPinned);
        ClassAnnouncement updated = classAnnouncementRepository.save(announcement);
        return toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassAnnouncementDto> getAnnouncementsByClass(Long classId) {
        if (!classesRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId);
        }
        return classAnnouncementRepository.findByClassesIdOrderByIsPinnedDescCreatedAtDesc(classId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassAnnouncementDto> getAnnouncementsByClassPaged(Long classId, int page, int size) {
        if (!classesRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId);
        }
        Pageable pageable = PageRequest.of(page, size);
        return classAnnouncementRepository.findByClassesIdOrderByIsPinnedDescCreatedAtDesc(classId, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassAnnouncementDto getAnnouncementById(Long id) {
        ClassAnnouncement announcement = classAnnouncementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo với ID: " + id));
        return toDto(announcement);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long id) {
        ClassAnnouncement announcement = classAnnouncementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo với ID: " + id));

        User currentUser = resolveUser(null);
        checkTeacherClassPermission(currentUser, announcement.getClasses().getId());

        classAnnouncementRepository.delete(announcement);
    }

    private void checkTeacherClassPermission(User currentUser, Long classId) {
        if (classId == null) return;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (isAcademicDepartmentUser(currentUser, auth)) {
            return;
        }

        if (currentUser != null) {
            Long userId = currentUser.getId();
            Long staffId = staffRepository.findByUserId(userId).map(Staff::getId).orElse(null);

            boolean isAssigned = scheduleAssignmentRepository.isTeacherAssignedToClass(classId, userId, staffId);
            boolean isSubstituting = teachingSubstitutionRepository.isTeacherSubstitutingForClass(classId, userId, staffId);

            if (!isAssigned && !isSubstituting) {
                throw new OperationNotPermittedException("Bạn không được phân công giảng dạy lớp học này nên không có quyền thao tác thông báo!");
            }
        } else {
            throw new OperationNotPermittedException("Bạn cần đăng nhập để thực hiện thao tác thông báo!");
        }
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

    private User resolveUser(Long userId) {
        if (userId != null) {
            return userRepository.findById(userId).orElse(null);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    private ClassAnnouncementDto toDto(ClassAnnouncement entity) {
        Classes classes = entity.getClasses();
        User author = entity.getCreatedBy();

        String attUrl = entity.getAttachmentUrl();
        if (attUrl != null && !attUrl.trim().isEmpty() && !attUrl.startsWith("http")) {
            attUrl = minioStorageService.getFileUrl(attUrl);
        }

        return ClassAnnouncementDto.builder()
                .id(entity.getId())
                .classId(classes != null ? classes.getId() : null)
                .createdById(author != null ? author.getId() : null)
                .title(entity.getTitle())
                .content(entity.getContent())
                .hasAttachment(entity.getHasAttachment())
                .attachmentUrl(attUrl)
                .isPinned(entity.getIsPinned())
                .createdAt(entity.getCreatedAt())
                .className(classes != null ? classes.getName() : null)
                .classCode(classes != null ? classes.getCode() : null)
                .createdByName(author != null ? author.getFullName() : null)
                .createdByEmail(author != null ? author.getEmail() : null)
                .createdByRole(author != null && author.getRoles() != null && !author.getRoles().isEmpty()
                        ? author.getRoles().iterator().next().getName() : null)
                .build();
    }
}
