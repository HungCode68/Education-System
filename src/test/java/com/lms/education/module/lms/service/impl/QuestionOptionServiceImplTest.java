package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms.dto.QuestionOptionDto;
import com.lms.education.module.lms.entity.Question;
import com.lms.education.module.lms.entity.QuestionOption;
import com.lms.education.module.lms.repository.QuestionOptionRepository;
import com.lms.education.module.lms.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class QuestionOptionServiceImplTest {

    @Mock
    private QuestionOptionRepository questionOptionRepository;
    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private QuestionOptionServiceImpl questionOptionService;

    private Question mockQuestion;
    private QuestionOption mockOption;
    private QuestionOptionDto mockDto;

    @BeforeEach
    void setUp() {
        mockQuestion = new Question();
        mockQuestion.setId(1L);
        mockQuestion.setQuestionType("MULTIPLE_CHOICE");

        mockOption = new QuestionOption();
        mockOption.setId(10L);
        mockOption.setQuestion(mockQuestion);
        mockOption.setOptionContent("Option A");
        mockOption.setIsCorrect(true);

        mockDto = QuestionOptionDto.builder()
                .optionContent("Option A")
                .isCorrect(true)
                .build();
    }

    @Test
    void validateOptionsForQuestionType_Success() {
        assertDoesNotThrow(() -> QuestionOptionServiceImpl.validateOptionsForQuestionType("MULTIPLE_CHOICE", List.of(mockDto)));
    }

    @Test
    void validateOptionsForQuestionType_MultipleChoiceWithoutCorrect_ThrowsException() {
        mockDto.setIsCorrect(false);
        assertThrows(OperationNotPermittedException.class, 
                () -> QuestionOptionServiceImpl.validateOptionsForQuestionType("MULTIPLE_CHOICE", List.of(mockDto)));
    }

    @Test
    void getByQuestionId_Success() {
        when(questionRepository.existsById(1L)).thenReturn(true);
        when(questionOptionRepository.findByQuestionIdOrderByIdAsc(1L)).thenReturn(List.of(mockOption));

        List<QuestionOptionDto> result = questionOptionService.getByQuestionId(1L);

        assertEquals(1, result.size());
        assertEquals("Option A", result.get(0).getOptionContent());
    }

    @Test
    void getByQuestionId_NotFound_ThrowsException() {
        when(questionRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> questionOptionService.getByQuestionId(1L));
    }

    @Test
    void create_Success() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(questionOptionRepository.save(any(QuestionOption.class))).thenReturn(mockOption);

        QuestionOptionDto result = questionOptionService.create(1L, mockDto);

        assertNotNull(result);
        assertEquals("Option A", result.getOptionContent());
    }

    @Test
    void create_QuestionNotFound_ThrowsException() {
        when(questionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> questionOptionService.create(1L, mockDto));
    }

    @Test
    void create_EmptyContent_ThrowsException() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        mockDto.setOptionContent("   ");

        assertThrows(OperationNotPermittedException.class, () -> questionOptionService.create(1L, mockDto));
    }

    @Test
    void update_Success() {
        when(questionOptionRepository.findById(10L)).thenReturn(Optional.of(mockOption));
        when(questionOptionRepository.save(any(QuestionOption.class))).thenReturn(mockOption);

        QuestionOptionDto result = questionOptionService.update(10L, mockDto);

        assertNotNull(result);
    }

    @Test
    void update_OptionNotFound_ThrowsException() {
        when(questionOptionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> questionOptionService.update(10L, mockDto));
    }

    @Test
    void delete_Success() {
        when(questionOptionRepository.findById(10L)).thenReturn(Optional.of(mockOption));

        questionOptionService.delete(10L);

        verify(questionOptionRepository).delete(mockOption);
    }

    @Test
    void delete_OptionNotFound_ThrowsException() {
        when(questionOptionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> questionOptionService.delete(10L));
    }

    @Test
    void replaceAllForQuestion_Success() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
        when(questionOptionRepository.saveAll(anyList())).thenReturn(List.of(mockOption));

        List<QuestionOptionDto> result = questionOptionService.replaceAllForQuestion(1L, List.of(mockDto));

        assertEquals(1, result.size());
        verify(questionOptionRepository).deleteByQuestionId(1L);
        verify(questionOptionRepository).flush();
    }

    @Test
    void replaceAllForQuestion_QuestionNotFound_ThrowsException() {
        when(questionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> questionOptionService.replaceAllForQuestion(1L, List.of(mockDto)));
    }
    
    @Test
    void replaceAllForQuestion_EmptyList_Success() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));

        List<QuestionOptionDto> result = questionOptionService.replaceAllForQuestion(1L, List.of());

        assertEquals(0, result.size());
        verify(questionOptionRepository).deleteByQuestionId(1L);
        verify(questionOptionRepository).flush();
    }
}
