package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms.dto.AssignmentQuestionDto;
import com.lms.education.module.lms.entity.Assignment;
import com.lms.education.module.lms.entity.AssignmentQuestion;
import com.lms.education.module.lms.entity.AssignmentQuestionId;
import com.lms.education.module.lms.entity.Question;
import com.lms.education.module.lms.repository.AssignmentQuestionRepository;
import com.lms.education.module.lms.repository.AssignmentRepository;
import com.lms.education.module.lms.repository.QuestionOptionRepository;
import com.lms.education.module.lms.repository.QuestionRepository;
import com.lms.education.module.lms.repository.SubmissionRepository;
import com.lms.education.service.MinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AssignmentQuestionServiceImplTest {

    @Mock
    private AssignmentQuestionRepository assignmentQuestionRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionOptionRepository questionOptionRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private MinioStorageService minioStorageService;

    @InjectMocks
    private AssignmentQuestionServiceImpl assignmentQuestionService;

    private Assignment mockAssignment;
    private Question mockQuestion;
    private AssignmentQuestion mockAssignmentQuestion;
    private AssignmentQuestionDto mockDto;
    private AssignmentQuestionId mockId;

    @BeforeEach
    void setUp() {
        mockAssignment = new Assignment();
        mockAssignment.setId(100L);

        mockQuestion = new Question();
        mockQuestion.setId(1L);

        mockId = new AssignmentQuestionId();
        mockId.setAssignmentId(100L);
        mockId.setQuestionId(1L);

        mockAssignmentQuestion = new AssignmentQuestion();
        mockAssignmentQuestion.setId(mockId);
        mockAssignmentQuestion.setAssignment(mockAssignment);
        mockAssignmentQuestion.setQuestion(mockQuestion);
        mockAssignmentQuestion.setOrderNumber(1);
        mockAssignmentQuestion.setScoreWeight(BigDecimal.ONE);

        mockDto = AssignmentQuestionDto.builder()
                .assignmentId(100L)
                .questionId(1L)
                .orderNumber(1)
                .scoreWeight(BigDecimal.ONE)
                .build();
    }

    @Test
    void getByAssignmentId_Success() {
        when(assignmentRepository.existsById(100L)).thenReturn(true);
        when(assignmentQuestionRepository.findByAssignmentIdOrderByOrderNumberAsc(100L))
                .thenReturn(List.of(mockAssignmentQuestion));

        List<AssignmentQuestionDto> result = assignmentQuestionService.getByAssignmentId(100L);

        assertEquals(1, result.size());
    }

    @Test
    void getByAssignmentId_NotFound_ThrowsException() {
        when(assignmentRepository.existsById(100L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> assignmentQuestionService.getByAssignmentId(100L));
    }

    @Test
    void addQuestionToAssignment_Success() {
        when(submissionRepository.existsByAssignmentId(100L)).thenReturn(false);
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(mockAssignment));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.existsByAssignmentIdAndQuestionId(100L, 1L)).thenReturn(false);
        when(assignmentQuestionRepository.save(any(AssignmentQuestion.class))).thenReturn(mockAssignmentQuestion);

        AssignmentQuestionDto result = assignmentQuestionService.addQuestionToAssignment(100L, mockDto);

        assertNotNull(result);
    }

    @Test
    void addQuestionToAssignment_HasSubmissions_ThrowsException() {
        when(submissionRepository.existsByAssignmentId(100L)).thenReturn(true);

        assertThrows(OperationNotPermittedException.class, 
                () -> assignmentQuestionService.addQuestionToAssignment(100L, mockDto));
    }

    @Test
    void addQuestionToAssignment_QuestionAlreadyExists_ThrowsException() {
        when(submissionRepository.existsByAssignmentId(100L)).thenReturn(false);
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(mockAssignment));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.existsByAssignmentIdAndQuestionId(100L, 1L)).thenReturn(true);

        assertThrows(OperationNotPermittedException.class, 
                () -> assignmentQuestionService.addQuestionToAssignment(100L, mockDto));
    }

    @Test
    void addQuestionToAssignment_InvalidWeight_ThrowsException() {
        when(submissionRepository.existsByAssignmentId(100L)).thenReturn(false);
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(mockAssignment));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.existsByAssignmentIdAndQuestionId(100L, 1L)).thenReturn(false);

        mockDto.setScoreWeight(new BigDecimal("-1"));

        assertThrows(OperationNotPermittedException.class, 
                () -> assignmentQuestionService.addQuestionToAssignment(100L, mockDto));
    }

    @Test
    void updateQuestionInAssignment_Success() {
        when(submissionRepository.existsByAssignmentId(100L)).thenReturn(false);
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 1L))
                .thenReturn(Optional.of(mockAssignmentQuestion));
        when(assignmentQuestionRepository.save(any(AssignmentQuestion.class))).thenReturn(mockAssignmentQuestion);

        AssignmentQuestionDto result = assignmentQuestionService.updateQuestionInAssignment(100L, 1L, 2, new BigDecimal("2"));

        assertNotNull(result);
        assertEquals(2, mockAssignmentQuestion.getOrderNumber());
        assertEquals(new BigDecimal("2"), mockAssignmentQuestion.getScoreWeight());
    }

    @Test
    void removeQuestionFromAssignment_Success() {
        when(submissionRepository.existsByAssignmentId(100L)).thenReturn(false);
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 1L))
                .thenReturn(Optional.of(mockAssignmentQuestion));

        assignmentQuestionService.removeQuestionFromAssignment(100L, 1L);

        verify(assignmentQuestionRepository).delete(mockAssignmentQuestion);
    }

    @Test
    void batchReplaceAssignmentQuestions_Success() {
        when(submissionRepository.existsByAssignmentId(100L)).thenReturn(false);
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(mockAssignment));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.saveAll(anyList())).thenReturn(List.of(mockAssignmentQuestion));

        List<AssignmentQuestionDto> result = assignmentQuestionService.batchReplaceAssignmentQuestions(100L, List.of(mockDto));

        assertEquals(1, result.size());
        verify(assignmentQuestionRepository).deleteByAssignmentId(100L);
    }

    @Test
    void batchReplaceAssignmentQuestions_EmptyList_Success() {
        when(submissionRepository.existsByAssignmentId(100L)).thenReturn(false);
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(mockAssignment));

        List<AssignmentQuestionDto> result = assignmentQuestionService.batchReplaceAssignmentQuestions(100L, List.of());

        assertEquals(0, result.size());
    }

    @Test
    void batchReplaceAssignmentQuestions_DuplicateQuestion_ThrowsException() {
        when(submissionRepository.existsByAssignmentId(100L)).thenReturn(false);
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(mockAssignment));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));

        AssignmentQuestionDto duplicateDto = AssignmentQuestionDto.builder().questionId(1L).build();

        assertThrows(OperationNotPermittedException.class, 
                () -> assignmentQuestionService.batchReplaceAssignmentQuestions(100L, List.of(mockDto, duplicateDto)));
    }
}
