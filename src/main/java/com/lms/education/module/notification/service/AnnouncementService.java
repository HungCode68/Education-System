package com.lms.education.module.notification.service;

import com.lms.education.module.notification.dto.AnnouncementDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface AnnouncementService {

    // Đăng thông báo mới (Có hỗ trợ đính kèm file)
    AnnouncementDto createAnnouncement(AnnouncementDto dto, MultipartFile file, String userId);

    // Lấy danh sách thông báo của Lớp Offline
    Page<AnnouncementDto> getAnnouncementsByPhysicalClass(String classId, Pageable pageable);

    // Lấy danh sách thông báo của Lớp Online
    Page<AnnouncementDto> getAnnouncementsByOnlineClass(String classId, Pageable pageable);

    AnnouncementDto updateAnnouncement(String announcementId, AnnouncementDto dto, MultipartFile file, String userId);

    // Xóa thông báo (Xóa luôn cả file đính kèm trên MinIO nếu có)
    void deleteAnnouncement(String announcementId, String userId);
}
