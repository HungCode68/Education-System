package com.lms.education.integration;

import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.lms.dto.LessonDto;
import com.lms.education.module.lms.entity.Lesson;
import com.lms.education.module.lms.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class LessonIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private CourseRepository courseRepository;

    private Lesson testLesson;
    private Classes testClass;

    @BeforeEach
    void setUp() {
        // Prepare Course
        List<Course> courses = courseRepository.findAll();
        Course testCourse;
        if (courses.isEmpty()) {
            Course course = new Course();
            course.setCode("COURSE_LMS");
            course.setName("LMS Test Course");
            course.setBasePrice(new java.math.BigDecimal("1200000"));
            testCourse = courseRepository.save(course);
        } else {
            testCourse = courses.get(0);
        }

        // Prepare Classes
        List<Classes> classesList = classesRepository.findAll();
        if (classesList.isEmpty()) {
            Classes newClass = new Classes();
            newClass.setCode("CLASS_LMS");
            newClass.setName("LMS Test Class");
            newClass.setCourse(testCourse);
            newClass.setMaxStudents(30);
            newClass.setStatus("OPENING");
            testClass = classesRepository.save(newClass);
        } else {
            testClass = classesList.get(0);
        }

        // Prepare Lesson
        List<Lesson> lessons = lessonRepository.findAll();
        if (lessons.isEmpty()) {
            Lesson lesson = new Lesson();
            lesson.setClasses(testClass);
            lesson.setName("Test Lesson 1");
            lesson.setOrderNumber(1);
            lesson.setDescription("Introduction to Test");
            testLesson = lessonRepository.save(lesson);
        } else {
            testLesson = lessons.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"LMS_LESSON_VIEW"})
    void testGetLessonById() throws Exception {
        mockMvc.perform(get("/api/v1/lessons/" + testLesson.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testLesson.getId()));
    }

    @Test
    @WithMockUser(authorities = {"LMS_LESSON_CREATE"})
    void testCreateLesson() throws Exception {
        LessonDto newLesson = new LessonDto();
        newLesson.setClassId(testClass.getId());
        newLesson.setName("Test Lesson 2");
        newLesson.setOrderNumber(2);
        newLesson.setDescription("Second test lesson");

        mockMvc.perform(post("/api/v1/lessons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newLesson)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tạo bài học thành công!"))
                .andExpect(jsonPath("$.data.name").value("Test Lesson 2"));
    }

    @Test
    @WithMockUser(authorities = {"LMS_LESSON_UPDATE"})
    void testUpdateLesson() throws Exception {
        LessonDto updateDto = new LessonDto();
        updateDto.setClassId(testClass.getId());
        updateDto.setName("Test Lesson 1 Updated");
        updateDto.setOrderNumber(1);
        updateDto.setDescription("Updated description");

        mockMvc.perform(put("/api/v1/lessons/" + testLesson.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật bài học thành công!"))
                .andExpect(jsonPath("$.data.name").value("Test Lesson 1 Updated"));
    }

    @Test
    @WithMockUser(authorities = {"LMS_LESSON_DELETE"})
    void testDeleteLesson() throws Exception {
        mockMvc.perform(delete("/api/v1/lessons/" + testLesson.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa bài học thành công!"));
    }
}
