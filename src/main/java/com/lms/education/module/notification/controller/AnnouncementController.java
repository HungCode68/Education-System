package com.lms.education.module.notification.controller;

import com.lms.education.module.notification.dto.AnnouncementDto;
import com.lms.education.module.notification.service.AnnouncementService;
import com.lms.education.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
@Slf4j
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * GIÁO VIÊN / ADMIN: ĐĂNG THÔNG BÁO MỚI (Hỗ trợ đính kèm file MinIO)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CLASS_UPDATE') or hasAnyRole('SUBJECT_TEACHER', 'SYSTEM_ADMIN')") // Bạn nhớ điều chỉnh quyền cho khớp với hệ thống của bạn nhé
    public ResponseEntity<AnnouncementDto> createAnnouncement(
            @RequestPart("data") AnnouncementDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Principal principal) {

        String userId = getUserId(principal);
        log.info("REST request - User {} đang đăng thông báo mới cho scope {}", userId, dto.getScope());

        AnnouncementDto createdAnnouncement = announcementService.createAnnouncement(dto, file, userId);
        return new ResponseEntity<>(createdAnnouncement, HttpStatus.CREATED);
    }

    /**
     * CHUNG: LẤY DANH SÁCH THÔNG BÁO CỦA LỚP OFFLINE (Phân trang)
     */
    @GetMapping("/physical-class/{classId}")
    @PreAuthorize("hasAuthority('CLASS_VIEW') or hasAnyRole('STUDENT', 'SUBJECT_TEACHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<Page<AnnouncementDto>> getAnnouncementsByPhysicalClass(
            @PathVariable String classId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("REST request - Lấy danh sách thông báo lớp Offline: {}", classId);
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<AnnouncementDto> announcements = announcementService.getAnnouncementsByPhysicalClass(classId, pageable);
        return ResponseEntity.ok(announcements);
    }

    /**
     * CHUNG: LẤY DANH SÁCH THÔNG BÁO CỦA LỚP ONLINE (Phân trang)
     */
    @GetMapping("/online-class/{classId}")
    @PreAuthorize("hasAuthority('CLASS_VIEW') or hasAnyRole('STUDENT', 'SUBJECT_TEACHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<Page<AnnouncementDto>> getAnnouncementsByOnlineClass(
            @PathVariable String classId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("REST request - Lấy danh sách thông báo lớp Online: {}", classId);
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<AnnouncementDto> announcements = announcementService.getAnnouncementsByOnlineClass(classId, pageable);
        return ResponseEntity.ok(announcements);
    }

    /**
     * GIÁO VIÊN / ADMIN: CẬP NHẬT THÔNG BÁO (Hỗ trợ thay đổi file đính kèm)
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CLASS_UPDATE') or hasAnyRole('HOMEROOM_TEACHER')")
    public ResponseEntity<AnnouncementDto> updateAnnouncement(
            @PathVariable String id,
            @RequestPart("data") AnnouncementDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Principal principal) {

        String userId = getUserId(principal);
        log.info("REST request - User {} đang cập nhật thông báo ID: {}", userId, id);

        AnnouncementDto updatedAnnouncement = announcementService.updateAnnouncement(id, dto, file, userId);
        return ResponseEntity.ok(updatedAnnouncement);
    }

    /**
     * GIÁO VIÊN / ADMIN: XÓA THÔNG BÁO
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLASS_UPDATE') or hasAnyRole('SUBJECT_TEACHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteAnnouncement(
            @PathVariable String id,
            Principal principal) {

        String userId = getUserId(principal);
        log.info("REST request - User {} đang xóa thông báo ID: {}", userId, id);

        announcementService.deleteAnnouncement(id, userId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa thông báo và giải phóng file đính kèm thành công"));
    }

    // --- Hàm Helper lấy ID từ Token ---
    private String getUserId(Principal principal) {
        UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();
        return userPrincipal.getId();
    }
}
