package com.lms.education.module.notification.service;

import com.lms.education.module.notification.dto.ClassAnnouncementDto;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ClassAnnouncementService {

    ClassAnnouncementDto createAnnouncement(ClassAnnouncementDto dto);

    ClassAnnouncementDto createAnnouncementWithFile(Long classId, String title, String content, Boolean isPinned, MultipartFile file);

    ClassAnnouncementDto updateAnnouncement(Long id, String title, String content, Boolean isPinned, Boolean removeAttachment, MultipartFile file);

    ClassAnnouncementDto togglePin(Long id, boolean isPinned);

    List<ClassAnnouncementDto> getAnnouncementsByClass(Long classId);

    Page<ClassAnnouncementDto> getAnnouncementsByClassPaged(Long classId, int page, int size);

    ClassAnnouncementDto getAnnouncementById(Long id);

    void deleteAnnouncement(Long id);
}
