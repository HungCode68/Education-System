package com.lms.education.module.lms.service;

import com.lms.education.module.lms.dto.QuestionImportResultDto;
import org.springframework.web.multipart.MultipartFile;

public interface QuestionExcelService {

    byte[] generateQuestionImportTemplate();

    QuestionImportResultDto importQuestionsFromExcel(MultipartFile file);

    QuestionImportResultDto importQuestionsFromExcel(MultipartFile file, Long assignmentId);
}
