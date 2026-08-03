package com.lms.education.module.lms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionDto {

    private Long id;

    @NotBlank(message = "Loại câu hỏi (questionType) không được để trống")
    private String questionType; // MULTIPLE_CHOICE, ESSAY, LISTENING, READING, FILL_BLANK, TRUE_FALSE

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String content;

    private String mediaUrl;

    private String downloadMediaUrl; // Đường dẫn tải/nghe file đính kèm từ MinIO

    private String readingPassage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private List<QuestionOptionDto> options;
}

