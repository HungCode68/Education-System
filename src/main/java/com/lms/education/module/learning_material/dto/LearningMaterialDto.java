package com.lms.education.module.learning_material.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.education.module.learning_material.entity.LearningMaterial;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LearningMaterialDto {

    private String id;

    // --- LIÊN KẾT LỚP HỌC ---
    @NotBlank(message = "Vui lòng chọn lớp học trực tuyến")
    private String onlineClassId;
    private String onlineClassName; // Dùng để hiển thị

    // --- CHI TIẾT TÀI LIỆU ---
    @NotBlank(message = "Tiêu đề tài liệu không được để trống")
    private String title;

    private String description;

    // Thông tin tệp
    private LearningMaterial.FileType fileType;
    private String filePath;
    private Long fileSize;
    private String fileName;

    // Trạng thái (Published/Unpublished)
    private LearningMaterial.MaterialStatus status;

    // --- NGƯỜI TẢI LÊN ---
    private String uploadedById;
    private String uploadedByName;

    // --- AUDIT ---
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
