package com.lms.education.integration;

import com.lms.education.module.lms.dto.QuestionDto;
import com.lms.education.module.lms.dto.QuestionOptionDto;
import com.lms.education.module.lms.entity.Question;
import com.lms.education.module.lms.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class QuestionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private QuestionRepository questionRepository;

    private Question testQuestion;

    @BeforeEach
    void setUp() {
        List<Question> questions = questionRepository.findAll();
        if (questions.isEmpty()) {
            Question question = new Question();
            question.setQuestionType("MULTIPLE_CHOICE");
            question.setContent("What is 1 + 1?");
            testQuestion = questionRepository.save(question);
        } else {
            testQuestion = questions.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"LMS_QUESTION_VIEW"})
    void testGetQuestionById() throws Exception {
        mockMvc.perform(get("/api/v1/questions/" + testQuestion.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testQuestion.getId()));
    }

    @Test
    @WithMockUser(authorities = {"LMS_QUESTION_CREATE"})
    void testCreateQuestion() throws Exception {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionType("MULTIPLE_CHOICE");
        QuestionOptionDto option1 = new QuestionOptionDto();
        option1.setOptionContent("A framework");
        option1.setIsCorrect(true);
        
        mockMvc.perform(multipart("/api/v1/questions")
                .param("questionType", "MULTIPLE_CHOICE")
                .param("content", "Test Question Content")
                .param("difficultyLevel", "EASY")
                .param("status", "ACTIVE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tạo mới câu hỏi thành công!"))
                .andExpect(jsonPath("$.data.content").value("Test Question Content"));
    }

    @Test
    @WithMockUser(authorities = {"LMS_QUESTION_UPDATE"})
    void testUpdateQuestion() throws Exception {
        mockMvc.perform(multipart("/api/v1/questions/" + testQuestion.getId())
                .with(request -> { request.setMethod("PUT"); return request; })
                .param("questionType", "MULTIPLE_CHOICE")
                .param("content", "Updated Question Content")
                .param("difficultyLevel", "MEDIUM")
                .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật câu hỏi thành công!"))
                .andExpect(jsonPath("$.data.content").value("Updated Question Content"));
    }

    @Test
    @WithMockUser(authorities = {"LMS_QUESTION_DELETE"})
    void testDeleteQuestion() throws Exception {
        mockMvc.perform(delete("/api/v1/questions/" + testQuestion.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa câu hỏi thành công!"));
    }
}
