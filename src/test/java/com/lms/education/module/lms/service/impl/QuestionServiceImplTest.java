package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms.dto.QuestionDto;
import com.lms.education.module.lms.dto.QuestionOptionDto;
import com.lms.education.module.lms.entity.Assignment;
import com.lms.education.module.lms.entity.AssignmentQuestion;
import com.lms.education.module.lms.entity.Question;
import com.lms.education.module.lms.entity.QuestionOption;
import com.lms.education.module.lms.repository.AssignmentQuestionRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class QuestionServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionOptionRepository questionOptionRepository;
    @Mock
    private MinioStorageService minioStorageService;
    @Mock
    private AssignmentQuestionRepository assignmentQuestionRepository;
    @Mock
    private SubmissionRepository submissionRepository;

    @InjectMocks
    private QuestionServiceImpl questionService;

    private Question mockQuestion;
    private QuestionDto mockDto;
    private MultipartFile mockFile;
    private AssignmentQuestion mockAssignmentQuestion;
    private Assignment mockAssignment;

    @BeforeEach
    void setUp() {
        mockQuestion = new Question();
        mockQuestion.setId(1L);
        mockQuestion.setQuestionType("MULTIPLE_CHOICE");
        mockQuestion.setContent("Test Question");

        QuestionOptionDto optionDto = QuestionOptionDto.builder()
                .optionContent("Option A")
                .isCorrect(true)
                .build();

        mockDto = QuestionDto.builder()
                .questionType("MULTIPLE_CHOICE")
                .content("Test Question")
                .options(List.of(optionDto))
                .build();

        mockFile = mock(MultipartFile.class);

        mockAssignment = new Assignment();
        mockAssignment.setId(100L);

        mockAssignmentQuestion = new AssignmentQuestion();
        mockAssignmentQuestion.setAssignment(mockAssignment);
        mockAssignmentQuestion.setQuestion(mockQuestion);
    }

    @Test
    void create_Success_MultipleChoice() {
        when(questionRepository.save(any(Question.class))).thenReturn(mockQuestion);

        QuestionDto result = questionService.create(mockDto, null);

        assertNotNull(result);
        assertEquals("MULTIPLE_CHOICE", result.getQuestionType());
    }

    @Test
    void create_Success_Reading() {
        mockDto.setQuestionType("READING");
        mockDto.setReadingPassage("This is a passage.");
        when(questionRepository.save(any(Question.class))).thenReturn(mockQuestion);

        QuestionDto result = questionService.create(mockDto, null);

        assertNotNull(result);
    }

    @Test
    void create_InvalidType_ThrowsException() {
        mockDto.setQuestionType("INVALID_TYPE");

        assertThrows(OperationNotPermittedException.class, () -> questionService.create(mockDto, null));
    }

    @Test
    void create_ReadingWithoutPassage_ThrowsException() {
        mockDto.setQuestionType("READING");
        mockDto.setReadingPassage("");

        assertThrows(OperationNotPermittedException.class, () -> questionService.create(mockDto, null));
    }

    @Test
    void create_ListeningWithoutMedia_ThrowsException() {
        mockDto.setQuestionType("LISTENING");
        mockDto.setMediaUrl("");

        assertThrows(OperationNotPermittedException.class, () -> questionService.create(mockDto, null));
    }

    @Test
    void update_Success() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByQuestionId(1L)).thenReturn(List.of());
        when(questionRepository.save(any(Question.class))).thenReturn(mockQuestion);

        QuestionDto result = questionService.update(1L, mockDto, null);

        assertNotNull(result);
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    void update_HasSubmissions_ThrowsException() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByQuestionId(1L)).thenReturn(List.of(mockAssignmentQuestion));
        when(submissionRepository.existsByAssignmentId(100L)).thenReturn(true);

        assertThrows(OperationNotPermittedException.class, () -> questionService.update(1L, mockDto, null));
    }

    @Test
    void update_ReadingWithoutPassage_ThrowsException() {
        mockQuestion.setQuestionType("MULTIPLE_CHOICE");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByQuestionId(1L)).thenReturn(List.of());

        mockDto.setQuestionType("READING");
        mockDto.setReadingPassage(null);

        assertThrows(OperationNotPermittedException.class, () -> questionService.update(1L, mockDto, null));
    }

    @Test
    void update_ListeningWithoutMedia_ThrowsException() {
        mockQuestion.setQuestionType("MULTIPLE_CHOICE");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByQuestionId(1L)).thenReturn(List.of());

        mockDto.setQuestionType("LISTENING");
        mockDto.setMediaUrl(null);

        assertThrows(OperationNotPermittedException.class, () -> questionService.update(1L, mockDto, null));
    }

    @Test
    void delete_Success() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByQuestionId(1L)).thenReturn(List.of());

        questionService.delete(1L);

        verify(questionRepository).delete(mockQuestion);
    }
    
    @Test
    void delete_HasSubmissions_ThrowsException() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByQuestionId(1L)).thenReturn(List.of(mockAssignmentQuestion));
        when(submissionRepository.existsByAssignmentId(100L)).thenReturn(true);

        assertThrows(OperationNotPermittedException.class, () -> questionService.delete(1L));
    }

    @Test
    void getById_Success() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        QuestionDto result = questionService.getById(1L);
        assertEquals("Test Question", result.getContent());
    }

    @Test
    void getByAssignmentId_Success() {
        when(questionRepository.findByAssignmentId(100L)).thenReturn(List.of(mockQuestion));
        List<QuestionDto> result = questionService.getByAssignmentId(100L);
        assertEquals(1, result.size());
    }

    @Test
    void getAll_Success() {
        Page<Question> page = new PageImpl<>(List.of(mockQuestion));
        Pageable pageable = PageRequest.of(0, 10);
        when(questionRepository.findAll(pageable)).thenReturn(page);

        Page<QuestionDto> result = questionService.getAll(null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }
    
    @Test
    void getAll_WithKeywordAndType_Success() {
        Page<Question> page = new PageImpl<>(List.of(mockQuestion));
        Pageable pageable = PageRequest.of(0, 10);
        when(questionRepository.findByKeywordAndType("test", "MULTIPLE_CHOICE", pageable)).thenReturn(page);

        Page<QuestionDto> result = questionService.getAll("test", "MULTIPLE_CHOICE", pageable);

        assertEquals(1, result.getTotalElements());
    }
    
    @Test
    void getAll_WithKeywordOnly_Success() {
        Page<Question> page = new PageImpl<>(List.of(mockQuestion));
        Pageable pageable = PageRequest.of(0, 10);
        when(questionRepository.findByKeyword("test", pageable)).thenReturn(page);

        Page<QuestionDto> result = questionService.getAll("test", null, pageable);

        assertEquals(1, result.getTotalElements());
    }
    
    @Test
    void getAll_WithTypeOnly_Success() {
        Page<Question> page = new PageImpl<>(List.of(mockQuestion));
        Pageable pageable = PageRequest.of(0, 10);
        when(questionRepository.findByQuestionTypeIgnoreCase("MULTIPLE_CHOICE", pageable)).thenReturn(page);

        Page<QuestionDto> result = questionService.getAll(null, "MULTIPLE_CHOICE", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void create_MultipleChoice_NoCorrectOption_ThrowsException() {
        QuestionOptionDto optionDto = QuestionOptionDto.builder()
                .optionContent("Option A")
                .isCorrect(false)
                .build();
        mockDto.setOptions(List.of(optionDto));
        when(questionRepository.save(any(Question.class))).thenReturn(mockQuestion);

        assertThrows(OperationNotPermittedException.class, () -> questionService.create(mockDto, null));
    }

    @Test
    void update_ClearMediaUrl_Success() {
        mockQuestion.setMediaUrl("old_media.jpg");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByQuestionId(1L)).thenReturn(List.of());
        when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

        mockDto.setMediaUrl("CLEAR");

        QuestionDto result = questionService.update(1L, mockDto, null);

        assertNotNull(result);
        assertNull(mockQuestion.getMediaUrl());
        verify(minioStorageService).deleteFile("old_media.jpg");
    }

    // --- Extra tests for QuestionServiceImpl coverage ---

    @Test
    void create_NullQuestionType_ThrowsException() {
        mockDto.setQuestionType(null);
        assertThrows(OperationNotPermittedException.class, () -> questionService.create(mockDto, null));
    }

    @Test
    void create_EmptyOptions_SkipsOptionsLoop() {
        mockDto.setOptions(List.of());
        when(questionRepository.save(any(Question.class))).thenReturn(mockQuestion);
        QuestionDto result = questionService.create(mockDto, null);
        assertNotNull(result);
        verify(questionOptionRepository, never()).save(any());
    }

    @Test
    void create_OptionsWithNullContent_SkipsOption() {
        QuestionOptionDto opt1 = QuestionOptionDto.builder().optionContent(null).build();
        QuestionOptionDto opt2 = QuestionOptionDto.builder().optionContent("  ").build();
        QuestionOptionDto opt3 = QuestionOptionDto.builder().optionContent("Valid").isCorrect(true).build();
        mockDto.setOptions(List.of(opt1, opt2, opt3));
        
        when(questionRepository.save(any(Question.class))).thenReturn(mockQuestion);
        QuestionDto result = questionService.create(mockDto, null);
        
        assertNotNull(result);
        verify(questionOptionRepository, times(1)).save(any(QuestionOption.class));
    }

    @Test
    void update_EmptyContent_SkipsContentUpdate() {
        mockQuestion.setContent("Old Content");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByQuestionId(1L)).thenReturn(List.of());
        when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

        mockDto.setContent("   "); // empty content
        mockDto.setQuestionType(null); // skip type
        
        QuestionDto result = questionService.update(1L, mockDto, null);
        assertEquals("Old Content", mockQuestion.getContent());
    }

    @Test
    void update_OptionsWithNullContent_SkipsOption() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByQuestionId(1L)).thenReturn(List.of());
        when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

        QuestionOptionDto opt1 = QuestionOptionDto.builder().optionContent(null).build();
        QuestionOptionDto opt2 = QuestionOptionDto.builder().optionContent("Valid").isCorrect(true).build();
        mockDto.setOptions(List.of(opt1, opt2)); // missing option content in first, correct in second

        QuestionDto result = questionService.update(1L, mockDto, null);
        verify(questionOptionRepository, times(1)).save(any(QuestionOption.class));
    }

    @Test
    void update_EmptyOptionsList_DeletesOldOptions() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByQuestionId(1L)).thenReturn(List.of());
        when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

        mockDto.setOptions(List.of());

        QuestionDto result = questionService.update(1L, mockDto, null);
        verify(questionOptionRepository, times(1)).deleteByQuestionId(1L);
        verify(questionOptionRepository, never()).save(any(QuestionOption.class));
    }

    @Test
    void update_ClearMediaUrl_WhenExistingIsAlreadyNull_DoesNothing() {
        mockQuestion.setMediaUrl(null);
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByQuestionId(1L)).thenReturn(List.of());
        when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

        mockDto.setMediaUrl("null");

        QuestionDto result = questionService.update(1L, mockDto, null);
        verify(minioStorageService, never()).deleteFile(anyString());
        assertNull(mockQuestion.getMediaUrl());
    }

    @Test
    void mapToDto_WithExistingMediaUrl_SetsDownloadUrl() {
        mockQuestion.setMediaUrl("file.jpg");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(minioStorageService.getFileUrl("file.jpg")).thenReturn("http://download.url");
        
        QuestionDto result = questionService.getById(1L);
        assertEquals("http://download.url", result.getDownloadMediaUrl());
    }

    @Test
    void update_ReadingCheckAfterUpdate_ThrowsException() {
        // Initially MULTIPLE_CHOICE, changed to READING but readingPassage is null
        mockQuestion.setQuestionType("MULTIPLE_CHOICE");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(assignmentQuestionRepository.findByQuestionId(1L)).thenReturn(List.of());

        mockDto.setQuestionType("READING");
        mockDto.setReadingPassage("  "); // Blank passage

        assertThrows(OperationNotPermittedException.class, () -> questionService.update(1L, mockDto, null));
    }
}
