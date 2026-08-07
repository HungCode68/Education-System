package com.lms.education.module.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSourceDto {
    private Long chunkId;
    private Long documentId;
    private String title;
    private Integer chunkIndex;
    private Double similarityScore;
}
