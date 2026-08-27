package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms.dto.GradeAnswerDto;
import com.lms.education.module.lms.dto.SubmissionAnswerDto;
import com.lms.education.module.lms.entity.*;
import com.lms.education.module.lms.repository.*;
import com.lms.education.service.MinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SubmissionAnswerServiceImplTest {

    @Mock
    private SubmissionAnswerRepository submissionAnswerRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionOptionRepository questionOptionRepository;
    @Mock
    private AssignmentQuestionRepository assignmentQuestionRepository;
    @Mock
    private MinioStorageService minioStorageService;

    @InjectMocks
    private SubmissionAnswerServiceImpl submissionAnswerService;

    private Submission mockSubmission;
    private Assignment mockAssignment;
    private Question mockQuestion;
    private QuestionOption mockOption;
    private AssignmentQuestion mockAssignmentQuestion;
    private SubmissionAnswer mockAnswer;
    private SubmissionAnswerDto mockDto;

    @BeforeEach
    void setUp() {
        mockAssignment = new Assignment();
        mockAssignment.setId(100L);
        mockAssignment.setShowCorrectAnswers(true);

        mockSubmission = new Submission();
        mockSubmission.setId(1L);
        mockSubmission.setStatus("IN_PROGRESS");
        mockSubmission.setAssignment(mockAssignment);

        mockQuestion = new Question();
        mockQuestion.setId(10L);
        mockQuestion.setQuestionType("MULTIPLE_CHOICE");
        
        mockOption = new QuestionOption();
        mockOption.setId(1000L);
        mockOption.setQuestion(mockQuestion);
        mockOption.setIsCorrect(true);
        
        mockQuestion.setOptions(List.of(mockOption));

        mockAssignmentQuestion = new AssignmentQuestion();
        mockAssignmentQuestion.setAssignment(mockAssignment);
        mockAssignmentQuestion.setQuestion(mockQuestion);
        mockAssignmentQuestion.setScoreWeight(BigDecimal.ONE);

        mockAnswer = new SubmissionAnswer();
        mockAnswer.setId(5L);
        mockAnswer.setSubmission(mockSubmission);
        mockAnswer.setQuestion(mockQuestion);
        mockAnswer.setEarnedScore(BigDecimal.ZERO);

        mockDto = SubmissionAnswerDto.builder()
                .questionId(10L)
                .selectedOptionId(1000L)
                .build();
                
        // Setup SecurityContext
        Authentication authentication = mock(Authentication.class);
        GrantedAuthority authority = mock(GrantedAuthority.class);
        when(authority.getAuthority()).thenReturn("ROLE_TEACHER");
        doReturn(List.of(authority)).when(authentication).getAuthorities();
        
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getAnswersBySubmissionId_Success() {
        when(submissionRepository.existsById(1L)).thenReturn(true);
        when(submissionAnswerRepository.findBySubmissionId(1L)).thenReturn(List.of(mockAnswer));

        List<SubmissionAnswerDto> result = submissionAnswerService.getAnswersBySubmissionId(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getAnswersBySubmissionId_NotFound_ThrowsException() {
        when(submissionRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> submissionAnswerService.getAnswersBySubmissionId(1L));
    }

    @Test
    void saveOrUpdateAnswer_Success_MultipleChoice() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 10L)).thenReturn(Optional.of(mockAssignmentQuestion));
        when(questionOptionRepository.findById(1000L)).thenReturn(Optional.of(mockOption));
        when(submissionAnswerRepository.findBySubmissionIdAndQuestionId(1L, 10L)).thenReturn(Optional.of(mockAnswer));
        when(submissionAnswerRepository.save(any(SubmissionAnswer.class))).thenReturn(mockAnswer);

        SubmissionAnswerDto result = submissionAnswerService.saveOrUpdateAnswer(1L, mockDto);

        assertNotNull(result);
        verify(submissionAnswerRepository).save(any(SubmissionAnswer.class));
        verify(submissionRepository).save(mockSubmission);
    }
    
    @Test
    void saveOrUpdateAnswer_Success_MultipleAnswers() {
        mockDto.setSelectedOptionId(null);
        mockDto.setSelectedOptionIds("[1000]");
        
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 10L)).thenReturn(Optional.of(mockAssignmentQuestion));
        when(submissionAnswerRepository.findBySubmissionIdAndQuestionId(1L, 10L)).thenReturn(Optional.of(mockAnswer));
        when(submissionAnswerRepository.save(any(SubmissionAnswer.class))).thenReturn(mockAnswer);

        SubmissionAnswerDto result = submissionAnswerService.saveOrUpdateAnswer(1L, mockDto);

        assertNotNull(result);
        verify(submissionAnswerRepository).save(any(SubmissionAnswer.class));
    }

    @Test
    void saveOrUpdateAnswer_NotInProgress_ThrowsException() {
        mockSubmission.setStatus("SUBMITTED");
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));

        assertThrows(OperationNotPermittedException.class, () -> submissionAnswerService.saveOrUpdateAnswer(1L, mockDto));
    }

    @Test
    void saveOrUpdateAnswer_QuestionNotInAssignment_ThrowsException() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 10L)).thenReturn(Optional.empty());

        assertThrows(OperationNotPermittedException.class, () -> submissionAnswerService.saveOrUpdateAnswer(1L, mockDto));
    }

    @Test
    void batchSaveAnswers_Success() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 10L)).thenReturn(Optional.of(mockAssignmentQuestion));
        when(questionOptionRepository.findById(1000L)).thenReturn(Optional.of(mockOption));
        when(submissionAnswerRepository.findBySubmissionIdAndQuestionId(1L, 10L)).thenReturn(Optional.of(mockAnswer));
        when(submissionAnswerRepository.save(any(SubmissionAnswer.class))).thenReturn(mockAnswer);

        List<SubmissionAnswerDto> result = submissionAnswerService.batchSaveAnswers(1L, List.of(mockDto));

        assertEquals(1, result.size());
    }

    @Test
    void removeAnswer_Success() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(submissionAnswerRepository.findBySubmissionIdAndQuestionId(1L, 10L)).thenReturn(Optional.of(mockAnswer));

        submissionAnswerService.removeAnswer(1L, 10L);

        verify(submissionAnswerRepository).delete(mockAnswer);
    }
    
    @Test
    void removeAnswer_AnswerNotFound_ThrowsException() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(submissionAnswerRepository.findBySubmissionIdAndQuestionId(1L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> submissionAnswerService.removeAnswer(1L, 10L));
    }

    @Test
    void gradeAnswer_Success() {
        mockSubmission.setStatus("SUBMITTED");
        when(submissionAnswerRepository.findById(5L)).thenReturn(Optional.of(mockAnswer));
        when(submissionAnswerRepository.save(any(SubmissionAnswer.class))).thenReturn(mockAnswer);

        SubmissionAnswerDto result = submissionAnswerService.gradeAnswer(5L, new BigDecimal("1.0"));

        assertNotNull(result);
        verify(submissionRepository, atLeastOnce()).save(mockSubmission);
        assertEquals("GRADED", mockSubmission.getStatus());
    }

    @Test
    void gradeAnswer_InvalidScore_ThrowsException() {
        when(submissionAnswerRepository.findById(5L)).thenReturn(Optional.of(mockAnswer));

        assertThrows(OperationNotPermittedException.class, () -> submissionAnswerService.gradeAnswer(5L, new BigDecimal("-1.0")));
    }

    @Test
    void batchGradeAnswers_Success() {
        mockSubmission.setStatus("SUBMITTED");
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(submissionAnswerRepository.findById(5L)).thenReturn(Optional.of(mockAnswer));
        when(submissionAnswerRepository.save(any(SubmissionAnswer.class))).thenReturn(mockAnswer);

        GradeAnswerDto gradeDto = new GradeAnswerDto();
        gradeDto.setAnswerId(5L);
        gradeDto.setScore(new BigDecimal("1.0"));

        List<SubmissionAnswerDto> result = submissionAnswerService.batchGradeAnswers(1L, List.of(gradeDto));

        assertEquals(1, result.size());
        verify(submissionRepository, atLeastOnce()).save(mockSubmission);
        assertEquals("GRADED", mockSubmission.getStatus());
    }
    
    @Test
    void saveOrUpdateAnswer_EmptySelectedIds() {
        mockDto.setSelectedOptionId(null);
        mockDto.setSelectedOptionIds("  ");
        mockQuestion.setQuestionType("ESSAY"); // Not multiple choice
        
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 10L)).thenReturn(Optional.of(mockAssignmentQuestion));
        when(submissionAnswerRepository.findBySubmissionIdAndQuestionId(1L, 10L)).thenReturn(Optional.of(mockAnswer));
        when(submissionAnswerRepository.save(any(SubmissionAnswer.class))).thenAnswer(i -> i.getArgument(0));

        SubmissionAnswerDto result = submissionAnswerService.saveOrUpdateAnswer(1L, mockDto);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getEarnedScore());
        assertFalse(result.getIsAutoGraded()); // Essay is false
    }

    @Test
    void saveOrUpdateAnswer_MultipleChoice_NoAnswer() {
        mockDto.setSelectedOptionId(null);
        mockDto.setSelectedOptionIds(null);
        mockQuestion.setQuestionType("MULTIPLE_CHOICE");
        
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 10L)).thenReturn(Optional.of(mockAssignmentQuestion));
        when(submissionAnswerRepository.findBySubmissionIdAndQuestionId(1L, 10L)).thenReturn(Optional.of(mockAnswer));
        when(submissionAnswerRepository.save(any(SubmissionAnswer.class))).thenAnswer(i -> i.getArgument(0));

        SubmissionAnswerDto result = submissionAnswerService.saveOrUpdateAnswer(1L, mockDto);

        assertEquals(BigDecimal.ZERO, result.getEarnedScore());
        assertTrue(result.getIsAutoGraded());
    }

    @Test
    void saveOrUpdateAnswer_MultipleChoice_WrongAnswer() {
        mockDto.setSelectedOptionId(1001L); // Wrong option
        QuestionOption wrongOption = new QuestionOption();
        wrongOption.setId(1001L);
        wrongOption.setIsCorrect(false);
        wrongOption.setQuestion(mockQuestion);
        
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 10L)).thenReturn(Optional.of(mockAssignmentQuestion));
        when(questionOptionRepository.findById(1001L)).thenReturn(Optional.of(wrongOption));
        when(submissionAnswerRepository.findBySubmissionIdAndQuestionId(1L, 10L)).thenReturn(Optional.of(mockAnswer));
        when(submissionAnswerRepository.save(any(SubmissionAnswer.class))).thenAnswer(i -> i.getArgument(0));

        SubmissionAnswerDto result = submissionAnswerService.saveOrUpdateAnswer(1L, mockDto);

        assertEquals(BigDecimal.ZERO, result.getEarnedScore());
    }
    
    // --- Extra tests for multiple answers and edge cases ---

    @Test
    void saveOrUpdateAnswer_MultipleAnswers_WithPenalty() {
        // Option 1000 is correct, option 1001 is incorrect.
        QuestionOption wrongOption = new QuestionOption();
        wrongOption.setId(1001L);
        wrongOption.setIsCorrect(false);
        wrongOption.setQuestion(mockQuestion);
        
        // Options: 1000 (Correct), 1001 (Wrong)
        mockQuestion.setOptions(List.of(mockOption, wrongOption));

        // Select both. totalCorrectOptions = 1. correctCount = 1, selectedCount = 2
        // penalty = 2 - 1 = 1. scoreRatio = (1 - 1)/1 = 0
        mockDto.setSelectedOptionId(null);
        mockDto.setSelectedOptionIds("[1000, 1001]");
        
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 10L)).thenReturn(Optional.of(mockAssignmentQuestion));
        when(submissionAnswerRepository.findBySubmissionIdAndQuestionId(1L, 10L)).thenReturn(Optional.of(mockAnswer));
        when(submissionAnswerRepository.save(any(SubmissionAnswer.class))).thenAnswer(i -> i.getArgument(0));

        SubmissionAnswerDto result = submissionAnswerService.saveOrUpdateAnswer(1L, mockDto);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getEarnedScore()));
    }

    @Test
    void saveOrUpdateAnswer_MultipleAnswers_PartialScore() {
        // 1000 (Correct), 1001 (Correct), 1002 (Wrong)
        QuestionOption correctOption2 = new QuestionOption();
        correctOption2.setId(1001L);
        correctOption2.setIsCorrect(true);
        correctOption2.setQuestion(mockQuestion);

        QuestionOption wrongOption = new QuestionOption();
        wrongOption.setId(1002L);
        wrongOption.setIsCorrect(false);
        wrongOption.setQuestion(mockQuestion);

        mockQuestion.setOptions(List.of(mockOption, correctOption2, wrongOption));

        // Select 1000 (Correct) and 1002 (Wrong). totalCorrectOptions = 2.
        // correctCount = 1, selectedCount = 2
        // penalty = 2 - 1 = 1. scoreRatio = (1 - 1)/2 = 0
        // Wait, let's select 1000 (Correct) only.
        // correctCount = 1, selectedCount = 1. penalty = 1 - 1 = 0. scoreRatio = 1/2 = 0.5.
        mockDto.setSelectedOptionId(null);
        mockDto.setSelectedOptionIds("[1000]");
        
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 10L)).thenReturn(Optional.of(mockAssignmentQuestion));
        when(submissionAnswerRepository.findBySubmissionIdAndQuestionId(1L, 10L)).thenReturn(Optional.of(mockAnswer));
        when(submissionAnswerRepository.save(any(SubmissionAnswer.class))).thenAnswer(i -> i.getArgument(0));

        SubmissionAnswerDto result = submissionAnswerService.saveOrUpdateAnswer(1L, mockDto);

        assertNotNull(result);
        // weight is 1. 1 * 0.5 = 0.5
        assertEquals(new BigDecimal("0.5"), result.getEarnedScore());
    }

    @Test
    void saveOrUpdateAnswer_SingleOption_NotBelongToQuestion() {
        Question otherQuestion = new Question();
        otherQuestion.setId(99L);
        
        QuestionOption otherOption = new QuestionOption();
        otherOption.setId(2000L);
        otherOption.setIsCorrect(true);
        otherOption.setQuestion(otherQuestion);
        
        mockDto.setSelectedOptionId(2000L);
        
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 10L)).thenReturn(Optional.of(mockAssignmentQuestion));
        when(questionOptionRepository.findById(2000L)).thenReturn(Optional.of(otherOption));
        
        assertThrows(OperationNotPermittedException.class, () -> submissionAnswerService.saveOrUpdateAnswer(1L, mockDto));
    }

    @Test
    void batchGradeAnswers_LateStatus_GetsGraded() {
        mockSubmission.setStatus("LATE");
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(submissionAnswerRepository.findById(5L)).thenReturn(Optional.of(mockAnswer));
        when(submissionAnswerRepository.save(any(SubmissionAnswer.class))).thenReturn(mockAnswer);

        GradeAnswerDto gradeDto = new GradeAnswerDto();
        gradeDto.setAnswerId(5L);
        gradeDto.setScore(new BigDecimal("1.0"));

        List<SubmissionAnswerDto> result = submissionAnswerService.batchGradeAnswers(1L, List.of(gradeDto));

        assertEquals(1, result.size());
        verify(submissionRepository, atLeastOnce()).save(mockSubmission);
        assertEquals("GRADED", mockSubmission.getStatus());
    }

    @Test
    void batchGradeAnswers_InProgressStatus_NoChange() {
        // IN_PROGRESS shouldn't be graded by this flow, but the code doesn't throw, it just updates the score and skips setting GRADED status.
        mockSubmission.setStatus("IN_PROGRESS");
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(submissionAnswerRepository.findById(5L)).thenReturn(Optional.of(mockAnswer));
        when(submissionAnswerRepository.save(any(SubmissionAnswer.class))).thenReturn(mockAnswer);

        GradeAnswerDto gradeDto = new GradeAnswerDto();
        gradeDto.setAnswerId(5L);
        gradeDto.setScore(new BigDecimal("1.0"));

        submissionAnswerService.batchGradeAnswers(1L, List.of(gradeDto));
        assertEquals("IN_PROGRESS", mockSubmission.getStatus());
    }
    
    @Test
    void batchSaveAnswers_NullList_ReturnsEmpty() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        List<SubmissionAnswerDto> result = submissionAnswerService.batchSaveAnswers(1L, null);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void batchGradeAnswers_NullList_ReturnsEmpty() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        List<SubmissionAnswerDto> result = submissionAnswerService.batchGradeAnswers(1L, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void batchGradeAnswers_InvalidScore_ThrowsException() {
        mockSubmission.setStatus("SUBMITTED");
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        
        GradeAnswerDto gradeDto = new GradeAnswerDto();
        gradeDto.setAnswerId(5L);
        gradeDto.setScore(new BigDecimal("-1.0"));

        assertThrows(OperationNotPermittedException.class, () -> submissionAnswerService.batchGradeAnswers(1L, List.of(gradeDto)));
    }

    @Test
    void batchGradeAnswers_AnswerNotFound_Ignores() {
        mockSubmission.setStatus("SUBMITTED");
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        
        GradeAnswerDto gradeDto = new GradeAnswerDto();
        gradeDto.setAnswerId(999L);
        gradeDto.setScore(new BigDecimal("1.0"));

        when(submissionAnswerRepository.findById(999L)).thenReturn(Optional.empty());

        List<SubmissionAnswerDto> result = submissionAnswerService.batchGradeAnswers(1L, List.of(gradeDto));
        assertTrue(result.isEmpty());
    }

    @Test
    void batchGradeAnswers_AnswerBelongsToOtherSubmission_Ignores() {
        mockSubmission.setStatus("SUBMITTED");
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        
        GradeAnswerDto gradeDto = new GradeAnswerDto();
        gradeDto.setAnswerId(5L);
        gradeDto.setScore(new BigDecimal("1.0"));

        Submission otherSubmission = new Submission();
        otherSubmission.setId(2L);
        SubmissionAnswer otherAnswer = new SubmissionAnswer();
        otherAnswer.setId(5L);
        otherAnswer.setSubmission(otherSubmission);

        when(submissionAnswerRepository.findById(5L)).thenReturn(Optional.of(otherAnswer));

        List<SubmissionAnswerDto> result = submissionAnswerService.batchGradeAnswers(1L, List.of(gradeDto));
        assertTrue(result.isEmpty());
    }

    @Test
    void toDto_AsStudent_Ungraded_HidesCorrectAnswers() {
        Authentication authentication = mock(Authentication.class);
        GrantedAuthority authority = mock(GrantedAuthority.class);
        when(authority.getAuthority()).thenReturn("ROLE_STUDENT");
        doReturn(List.of(authority)).when(authentication).getAuthorities();
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        mockSubmission.setStatus("SUBMITTED"); // Not GRADED
        when(submissionRepository.existsById(1L)).thenReturn(true);
        when(submissionAnswerRepository.findBySubmissionId(1L)).thenReturn(List.of(mockAnswer));

        List<SubmissionAnswerDto> result = submissionAnswerService.getAnswersBySubmissionId(1L);
        assertEquals(1, result.size());
        SubmissionAnswerDto dto = result.get(0);
        assertNull(dto.getCorrectOptionIds());
        assertNull(dto.getEarnedScore());
    }

    @Test
    void toDto_AsStudent_Graded_ShowsCorrectAnswersIfAllowed() {
        Authentication authentication = mock(Authentication.class);
        GrantedAuthority authority = mock(GrantedAuthority.class);
        when(authority.getAuthority()).thenReturn("ROLE_STUDENT");
        doReturn(List.of(authority)).when(authentication).getAuthorities();
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        mockSubmission.setStatus("GRADED");
        mockAssignment.setShowCorrectAnswers(true);
        when(submissionRepository.existsById(1L)).thenReturn(true);
        when(submissionAnswerRepository.findBySubmissionId(1L)).thenReturn(List.of(mockAnswer));
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 10L)).thenReturn(Optional.of(mockAssignmentQuestion));

        List<SubmissionAnswerDto> result = submissionAnswerService.getAnswersBySubmissionId(1L);
        assertEquals(1, result.size());
        SubmissionAnswerDto dto = result.get(0);
        assertNotNull(dto.getCorrectOptionIds());
        assertEquals(1, dto.getCorrectOptionIds().size());
        assertEquals(1000L, dto.getCorrectOptionIds().get(0));
    }

    @Test
    void toDto_AsStudent_Graded_HidesCorrectAnswersIfNotAllowed() {
        Authentication authentication = mock(Authentication.class);
        GrantedAuthority authority = mock(GrantedAuthority.class);
        when(authority.getAuthority()).thenReturn("ROLE_STUDENT");
        doReturn(List.of(authority)).when(authentication).getAuthorities();
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        mockSubmission.setStatus("GRADED");
        mockAssignment.setShowCorrectAnswers(false);
        when(submissionRepository.existsById(1L)).thenReturn(true);
        when(submissionAnswerRepository.findBySubmissionId(1L)).thenReturn(List.of(mockAnswer));

        List<SubmissionAnswerDto> result = submissionAnswerService.getAnswersBySubmissionId(1L);
        assertEquals(1, result.size());
        SubmissionAnswerDto dto = result.get(0);
        assertNull(dto.getCorrectOptionIds());
        assertNotNull(dto.getEarnedScore()); // Score is still visible since it's graded
    }

    @Test
    void toDto_WithMediaUrl_Success() throws Exception {
        mockQuestion.setMediaUrl("test/url.png");
        when(submissionRepository.existsById(1L)).thenReturn(true);
        when(submissionAnswerRepository.findBySubmissionId(1L)).thenReturn(List.of(mockAnswer));
        when(minioStorageService.getFileUrl("test/url.png")).thenReturn("http://minio/test/url.png");

        List<SubmissionAnswerDto> result = submissionAnswerService.getAnswersBySubmissionId(1L);
        assertEquals(1, result.size());
        assertEquals("http://minio/test/url.png", result.get(0).getQuestionAttachmentUrl());
    }

    @Test
    void toDto_WithMediaUrl_Exception_HandledGracefully() throws Exception {
        mockQuestion.setMediaUrl("test/url.png");
        when(submissionRepository.existsById(1L)).thenReturn(true);
        when(submissionAnswerRepository.findBySubmissionId(1L)).thenReturn(List.of(mockAnswer));
        when(minioStorageService.getFileUrl("test/url.png")).thenThrow(new RuntimeException("Minio error"));

        List<SubmissionAnswerDto> result = submissionAnswerService.getAnswersBySubmissionId(1L);
        assertEquals(1, result.size());
        assertNull(result.get(0).getQuestionAttachmentUrl());
    }

    @Test
    void batchSaveAnswers_WithNullQuestionId_Skips() {
        SubmissionAnswerDto nullQidDto = SubmissionAnswerDto.builder().build();
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        
        List<SubmissionAnswerDto> result = submissionAnswerService.batchSaveAnswers(1L, List.of(nullQidDto));
        assertTrue(result.isEmpty());
    }

    @Test
    void upsertAnswerInternal_MultipleAnswers_ParseError_HandledGracefully() {
        // Provide unparsable option ids
        mockDto.setSelectedOptionId(null);
        mockDto.setSelectedOptionIds("[1000,,abc]");
        
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByAssignmentIdAndQuestionId(100L, 10L)).thenReturn(Optional.of(mockAssignmentQuestion));
        when(submissionAnswerRepository.findBySubmissionIdAndQuestionId(1L, 10L)).thenReturn(Optional.of(mockAnswer));
        when(submissionAnswerRepository.save(any(SubmissionAnswer.class))).thenAnswer(i -> i.getArgument(0));

        SubmissionAnswerDto result = submissionAnswerService.saveOrUpdateAnswer(1L, mockDto);
        assertNotNull(result);
    }
}
