package com.lms.education.integration;

import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.lms.dto.AssignmentDto;
import com.lms.education.module.lms.entity.Assignment;
import com.lms.education.module.lms.entity.Lesson;
import com.lms.education.module.lms.repository.AssignmentRepository;
import com.lms.education.module.lms.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AssignmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private CourseRepository courseRepository;

    private Assignment testAssignment;
    private Lesson testLesson;

    @BeforeEach
    void setUp() {
        // Prepare Course
        List<Course> courses = courseRepository.findAll();
        Course testCourse;
        if (courses.isEmpty()) {
            Course course = new Course();
            course.setCode("COURSE_LMS_2");
            course.setName("LMS Test Course 2");
            course.setBasePrice(new java.math.BigDecimal("1200000"));
            testCourse = courseRepository.save(course);
        } else {
            testCourse = courses.get(0);
        }

        // Prepare Classes
        List<Classes> classesList = classesRepository.findAll();
        Classes testClass;
        if (classesList.isEmpty()) {
            Classes newClass = new Classes();
            newClass.setCode("CLASS_LMS_2");
            newClass.setName("LMS Test Class 2");
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
            lesson.setName("Test Lesson for Assignment");
            lesson.setOrderNumber(1);
            testLesson = lessonRepository.save(lesson);
        } else {
            testLesson = lessons.get(0);
        }

        // Prepare Assignment
        List<Assignment> assignments = assignmentRepository.findAll();
        if (assignments.isEmpty()) {
            Assignment assignment = new Assignment();
            assignment.setLesson(testLesson);
            assignment.setTitle("Test Assignment 1");
            assignment.setDueDate(LocalDateTime.now().plusDays(7));
            assignment.setAssignmentType("HOMEWORK");
            assignment.setStatus("PUBLISHED");
            assignment.setMaxAttempts(3);
            testAssignment = assignmentRepository.save(assignment);
        } else {
            testAssignment = assignments.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"LMS_ASSIGNMENT_VIEW"})
    void testGetAssignmentById() throws Exception {
        mockMvc.perform(get("/api/v1/assignments/" + testAssignment.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testAssignment.getId()));
    }

    @Test
    @WithMockUser(authorities = {"LMS_ASSIGNMENT_CREATE"})
    void testCreateAssignment() throws Exception {
        AssignmentDto newAssignment = new AssignmentDto();
        newAssignment.setLessonId(testLesson.getId());
        newAssignment.setTitle("Test Assignment 2");
        newAssignment.setDueDate(LocalDateTime.now().plusDays(5));
        newAssignment.setAssignmentType("QUIZ");
        newAssignment.setStatus("PUBLISHED");

        mockMvc.perform(post("/api/v1/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAssignment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tạo mới bài tập thành công!"))
                .andExpect(jsonPath("$.data.title").value("Test Assignment 2"));
    }

    @Test
    @WithMockUser(authorities = {"LMS_ASSIGNMENT_UPDATE"})
    void testUpdateAssignment() throws Exception {
        AssignmentDto updateDto = new AssignmentDto();
        updateDto.setLessonId(testLesson.getId());
        updateDto.setTitle("Test Assignment 1 Updated");
        updateDto.setDueDate(LocalDateTime.now().plusDays(10));
        updateDto.setAssignmentType("HOMEWORK");
        updateDto.setStatus("PUBLISHED");

        mockMvc.perform(put("/api/v1/assignments/" + testAssignment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật bài tập thành công!"))
                .andExpect(jsonPath("$.data.title").value("Test Assignment 1 Updated"));
    }

    @Test
    @WithMockUser(authorities = {"LMS_ASSIGNMENT_DELETE"})
    void testDeleteAssignment() throws Exception {
        mockMvc.perform(delete("/api/v1/assignments/" + testAssignment.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa bài tập thành công!"));
    }
}
