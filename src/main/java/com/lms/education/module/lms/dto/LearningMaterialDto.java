package com.lms.education.module.lms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LearningMaterialDto {

    private Long id;

    @NotNull(message = "ID bài học không được để trống")
    private Long lessonId;

    private String lessonName;
    private Long classId;
    private String className;

    @NotBlank(message = "Tiêu đề tài liệu không được để trống")
    private String title;

    @NotBlank(message = "Loại tài liệu không được để trống")
    private String materialType; // DOCUMENT, SLIDE, VIDEO, IMAGE, EXTERNAL_LINK

    private String sourceType; // MINIO, EXTERNAL

    private String resourceUrl; // Tên file trên MinIO hoặc URL ngoài

    private String downloadUrl; // URL động (Presigned URL của MinIO hoặc link ngoài) để Client tải/xem

    private Long fileSize;

    private Integer displayOrder;

    private Boolean isOfficial;
    private Boolean isRagEnabled;
    private String indexingStatus;

    private Long uploadedById;
    private String uploadedByName;
    private String uploadedByEmail;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
