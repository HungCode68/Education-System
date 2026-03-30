package com.lms.education.module.assignments.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lms.education.module.assignments.entity.SubmissionAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionAttachmentDto {

    private String id;

    private String submissionId;

    private String fileName;

    // Tên file gốc lưu trên MinIO
    private String filePath;

    // ĐƯỜNG LINK THỰC TẾ ĐỂ TẢI/XEM FILE TỪ MINIO
    private String fileUrl;

    private SubmissionAttachment.FileType fileType;

    private Long fileSize;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
