package com.lms.education.module.notification.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.notification.dto.ClassAnnouncementDto;
import com.lms.education.module.notification.service.ClassAnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/class-announcements")
@RequiredArgsConstructor
public class ClassAnnouncementController {

    private final ClassAnnouncementService classAnnouncementService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ANNOUNCEMENT_CREATE')")
    @LogActivity(module = "ANNOUNCEMENT", action = "CREATE", targetType = "class_announcement", description = "Tạo mới thông báo lớp học")
    public ResponseEntity<Map<String, Object>> createAnnouncement(@Valid @RequestBody ClassAnnouncementDto dto) {
        ClassAnnouncementDto saved = classAnnouncementService.createAnnouncement(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo thông báo lớp học thành công!");
        response.put("data", saved);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ANNOUNCEMENT_CREATE')")
    @LogActivity(module = "ANNOUNCEMENT", action = "CREATE", targetType = "class_announcement", description = "Tạo mới thông báo lớp học kèm tệp đính kèm")
    public ResponseEntity<Map<String, Object>> createAnnouncementWithFile(
            @RequestParam("classId") Long classId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "isPinned", defaultValue = "false") Boolean isPinned,
            @RequestPart("file") MultipartFile file) {

        ClassAnnouncementDto saved = classAnnouncementService.createAnnouncementWithFile(classId, title, content, isPinned, file);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo thông báo lớp học và tải tệp đính kèm thành công!");
        response.put("data", saved);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ANNOUNCEMENT_UPDATE')")
    @LogActivity(module = "ANNOUNCEMENT", action = "UPDATE", targetType = "class_announcement", description = "Cập nhật nội dung thông báo lớp học")
    public ResponseEntity<Map<String, Object>> updateAnnouncement(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Boolean isPinned,
            @RequestParam(required = false, defaultValue = "false") Boolean removeAttachment,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        
        ClassAnnouncementDto updated = classAnnouncementService.updateAnnouncement(id, title, content, isPinned, removeAttachment, file);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật thông báo lớp học thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/pin")
    @PreAuthorize("hasAnyAuthority('ANNOUNCEMENT_UPDATE')")
    @LogActivity(module = "ANNOUNCEMENT", action = "UPDATE", targetType = "class_announcement", description = "Ghim / bỏ ghim thông báo lớp học")
    public ResponseEntity<Map<String, Object>> togglePin(
            @PathVariable Long id,
            @RequestParam boolean isPinned) {
        ClassAnnouncementDto updated = classAnnouncementService.togglePin(id, isPinned);

        Map<String, Object> response = new HashMap<>();
        response.put("message", (isPinned ? "Ghim" : "Bỏ ghim") + " thông báo thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<ClassAnnouncementDto>> getAnnouncementsByClass(@PathVariable Long classId) {
        return ResponseEntity.ok(classAnnouncementService.getAnnouncementsByClass(classId));
    }

    @GetMapping("/class/{classId}/paged")
    public ResponseEntity<Page<ClassAnnouncementDto>> getAnnouncementsByClassPaged(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(classAnnouncementService.getAnnouncementsByClassPaged(classId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassAnnouncementDto> getAnnouncementById(@PathVariable Long id) {
        return ResponseEntity.ok(classAnnouncementService.getAnnouncementById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ANNOUNCEMENT_DELETE')")
    @LogActivity(module = "ANNOUNCEMENT", action = "DELETE", targetType = "class_announcement", description = "Xóa thông báo lớp học")
    public ResponseEntity<Map<String, Object>> deleteAnnouncement(@PathVariable Long id) {
        classAnnouncementService.deleteAnnouncement(id);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Xóa thông báo lớp học thành công!");

        return ResponseEntity.ok(response);
    }
}
