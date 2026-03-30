package com.lms.education.module.assignments.service;

import com.lms.education.module.assignments.dto.SubmissionAttachmentDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SubmissionAttachmentService {

    // Học sinh upload file đính kèm cho bài nộp của mình
    SubmissionAttachmentDto uploadAttachment(String submissionId, MultipartFile file, String userId);

    // Lấy danh sách file đính kèm của một bài nộp (Có sinh ra link tải từ MinIO)
    List<SubmissionAttachmentDto> getAttachmentsBySubmissionId(String submissionId);

    // Học sinh xóa file đính kèm (Xóa trong DB và xóa luôn trên MinIO)
    void deleteAttachment(String attachmentId, String userId);
}