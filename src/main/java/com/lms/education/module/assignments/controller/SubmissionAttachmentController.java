package com.lms.education.module.assignments.controller;

import com.lms.education.module.assignments.dto.SubmissionAttachmentDto;
import com.lms.education.module.assignments.service.SubmissionAttachmentService;
import com.lms.education.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/submission-attachments")
@RequiredArgsConstructor
@Slf4j
public class SubmissionAttachmentController {

    private final SubmissionAttachmentService attachmentService;

    /**
     * HỌC SINH: UPLOAD FILE ĐÍNH KÈM CHO BÀI NỘP
     * Yêu cầu truyền lên một file vật lý qua multipart/form-data
     */
    @PostMapping(value = "/submission/{submissionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('STUDENT_VIEW') or hasRole('STUDENT')")
    public ResponseEntity<SubmissionAttachmentDto> uploadAttachment(
            @PathVariable String submissionId,
            @RequestPart("file") MultipartFile file,
            Principal principal) {

        String userId = getUserId(principal);
        log.info("REST request - User {} đang upload file {} cho bài nộp {}", userId, file.getOriginalFilename(), submissionId);

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        SubmissionAttachmentDto uploadedAttachment = attachmentService.uploadAttachment(submissionId, file, userId);
        return new ResponseEntity<>(uploadedAttachment, HttpStatus.CREATED);
    }

    /**
     * LẤY DANH SÁCH FILE ĐÍNH KÈM
     * - Học sinh gọi để xem lại bài mình đã nộp.
     * - Giáo viên gọi để tải/xem bài của học sinh và chấm điểm.
     */
    @GetMapping("/submission/{submissionId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW') or hasAuthority('STUDENT_VIEW')")
    public ResponseEntity<List<SubmissionAttachmentDto>> getAttachments(
            @PathVariable String submissionId) {

        log.info("REST request - Lấy danh sách file đính kèm của bài nộp {}", submissionId);
        List<SubmissionAttachmentDto> attachments = attachmentService.getAttachmentsBySubmissionId(submissionId);

        return ResponseEntity.ok(attachments);
    }

    /**
     * HỌC SINH: XÓA FILE ĐÍNH KÈM
     * Hệ thống sẽ tự động gọi sang MinIO để xóa file vật lý, giải phóng dung lượng ổ cứng.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('STUDENT_VIEW') or hasRole('STUDENT')")
    public ResponseEntity<Map<String, String>> deleteAttachment(
            @PathVariable String id,
            Principal principal) {

        String userId = getUserId(principal);
        log.info("REST request - User {} đang xóa file đính kèm ID: {}", userId, id);

        attachmentService.deleteAttachment(id, userId);

        return ResponseEntity.ok(Map.of("message", "Đã xóa file đính kèm và giải phóng bộ nhớ thành công"));
    }

    // Hàm Helper lấy ID từ Token
    private String getUserId(Principal principal) {
        UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();
        return userPrincipal.getId();
    }
}