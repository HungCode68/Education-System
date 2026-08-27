package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.module.lms.dto.QuestionDto;
import com.lms.education.module.lms.dto.QuestionImportResultDto;
import com.lms.education.module.lms.service.AssignmentQuestionService;
import com.lms.education.module.lms.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class QuestionExcelServiceImplTest {

    @Mock
    private QuestionService questionService;
    @Mock
    private AssignmentQuestionService assignmentQuestionService;

    @InjectMocks
    private QuestionExcelServiceImpl questionExcelService;

    private byte[] validExcelBytes;

    @BeforeEach
    void setUp() {
        // We can use the service itself to generate a valid template for testing
        validExcelBytes = questionExcelService.generateQuestionImportTemplate();
    }

    @Test
    void generateQuestionImportTemplate_Success() {
        byte[] bytes = questionExcelService.generateQuestionImportTemplate();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void importQuestionsFromExcel_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
        assertThrows(OperationNotPermittedException.class, () -> questionExcelService.importQuestionsFromExcel(emptyFile));
    }

    @Test
    void importQuestionsFromExcel_InvalidExtension_ThrowsException() {
        MockMultipartFile invalidFile = new MockMultipartFile("file", "test.txt", "text/plain", "dummy".getBytes());
        assertThrows(OperationNotPermittedException.class, () -> questionExcelService.importQuestionsFromExcel(invalidFile));
    }

    @Test
    void importQuestionsFromExcel_Success() {
        MockMultipartFile validFile = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", validExcelBytes);
        
        QuestionDto mockSavedDto = new QuestionDto();
        mockSavedDto.setId(1L);
        when(questionService.create(any(), any())).thenReturn(mockSavedDto);

        QuestionImportResultDto result = questionExcelService.importQuestionsFromExcel(validFile);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
        
        verify(questionService, times(3)).create(any(), any());
    }

    @Test
    void importQuestionsFromExcel_WithAssignmentId_Success() {
        MockMultipartFile validFile = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", validExcelBytes);
        
        QuestionDto mockSavedDto = new QuestionDto();
        mockSavedDto.setId(1L);
        when(questionService.create(any(), any())).thenReturn(mockSavedDto);
        when(assignmentQuestionService.getByAssignmentId(100L)).thenReturn(List.of());

        QuestionImportResultDto result = questionExcelService.importQuestionsFromExcel(validFile, 100L);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        
        verify(questionService, times(3)).create(any(), any());
        verify(assignmentQuestionService, times(3)).addQuestionToAssignment(eq(100L), any());
    }
}
