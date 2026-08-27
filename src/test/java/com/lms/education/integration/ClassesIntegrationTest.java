package com.lms.education.integration;

import com.lms.education.module.academic.dto.ClassesDto;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ClassesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private CourseRepository courseRepository;

    private Classes testClass;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        // Prepare Course
        List<Course> courses = courseRepository.findAll();
        if (courses.isEmpty()) {
            Course course = new Course();
            course.setCode("COURSE_01");
            course.setName("Test Course");
            course.setBasePrice(new java.math.BigDecimal("1000000"));
            testCourse = courseRepository.save(course);
        } else {
            testCourse = courses.get(0);
        }

        // Prepare Classes
        List<Classes> classesList = classesRepository.findAll();
        if (classesList.isEmpty()) {
            Classes newClass = new Classes();
            newClass.setCode("CLASS_01");
            newClass.setName("Test Class 01");
            newClass.setCourse(testCourse);
            newClass.setMaxStudents(30);
            newClass.setStatus("OPENING");
            testClass = classesRepository.save(newClass);
        } else {
            testClass = classesList.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"CLASS_VIEW"})
    void testGetAllClasses() throws Exception {
        mockMvc.perform(get("/api/v1/classes")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(authorities = {"CLASS_CREATE"})
    void testCreateClass() throws Exception {
        ClassesDto newClass = new ClassesDto();
        newClass.setCourseId(testCourse.getId());
        newClass.setCode("CLASS_02");
        newClass.setName("Test Class 02");
        newClass.setMaxStudents(25);
        newClass.setStatus("OPENING");

        mockMvc.perform(post("/api/v1/classes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newClass)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tạo lớp học thành công!"))
                .andExpect(jsonPath("$.data.code").value("CLASS_02"));
    }

    @Test
    @WithMockUser(authorities = {"CLASS_UPDATE"})
    void testUpdateClass() throws Exception {
        ClassesDto updateDto = new ClassesDto();
        updateDto.setCourseId(testCourse.getId());
        updateDto.setCode("CLASS_01_UPDATED");
        updateDto.setName("Test Class 01 Updated");
        updateDto.setMaxStudents(40);
        updateDto.setStatus("ONGOING");

        mockMvc.perform(put("/api/v1/classes/" + testClass.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật thông tin lớp học thành công!"))
                .andExpect(jsonPath("$.data.code").value("CLASS_01_UPDATED"));
    }

    @Test
    @WithMockUser(authorities = {"CLASS_DELETE"})
    void testDeleteClass() throws Exception {
        mockMvc.perform(delete("/api/v1/classes/" + testClass.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa lớp học thành công!"));
    }
}
