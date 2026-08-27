package com.lms.education.integration;

import com.lms.education.module.lms.dto.LearningMaterialDto;
import com.lms.education.module.lms.entity.LearningMaterial;
import com.lms.education.module.lms.entity.Lesson;
import com.lms.education.module.lms.repository.LearningMaterialRepository;
import com.lms.education.module.lms.repository.LessonRepository;
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

public class LearningMaterialIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LearningMaterialRepository learningMaterialRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private CourseRepository courseRepository;

    private LearningMaterial testMaterial;
    private Lesson testLesson;

    @BeforeEach
    void setUp() {
        // Course
        List<Course> courses = courseRepository.findAll();
        Course testCourse;
        if (courses.isEmpty()) {
            Course course = new Course();
            course.setCode("COURSE_LM");
            course.setName("Learning Material Course");
            course.setBasePrice(new java.math.BigDecimal("2500000"));
            testCourse = courseRepository.save(course);
        } else {
            testCourse = courses.get(0);
        }

        // Classes
        List<Classes> classList = classesRepository.findAll();
        Classes testClass;
        if (classList.isEmpty()) {
            Classes clazz = new Classes();
            clazz.setCode("CLASS_LM");
            clazz.setName("Learning Material Class");
            clazz.setCourse(testCourse);
            clazz.setMaxStudents(30);
            clazz.setStatus("OPENING");
            testClass = classesRepository.save(clazz);
        } else {
            testClass = classList.get(0);
        }

        // Lesson
        List<Lesson> lessons = lessonRepository.findAll();
        if (lessons.isEmpty()) {
            Lesson lesson = new Lesson();
            lesson.setClasses(testClass);
            lesson.setName("Introduction to Learning Materials");
            lesson.setOrderNumber(1);
            testLesson = lessonRepository.save(lesson);
        } else {
            testLesson = lessons.get(0);
        }

        // Learning Material
        List<LearningMaterial> materials = learningMaterialRepository.findAll();
        if (materials.isEmpty()) {
            LearningMaterial material = new LearningMaterial();
            material.setLesson(testLesson);
            material.setTitle("Test Document");
            material.setMaterialType("DOCUMENT");
            material.setSourceType("EXTERNAL");
            material.setResourceUrl("http://example.com/doc.pdf");
            material.setDisplayOrder(1);
            testMaterial = learningMaterialRepository.save(material);
        } else {
            testMaterial = materials.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"MATERIAL_VIEW"})
    void testGetMaterialById() throws Exception {
        mockMvc.perform(get("/api/v1/learning-materials/" + testMaterial.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testMaterial.getId()));
    }

    @Test
    @WithMockUser(authorities = {"MATERIAL_CREATE"})
    void testCreateExternalLinkMaterial() throws Exception {
        LearningMaterialDto dto = new LearningMaterialDto();
        dto.setLessonId(testLesson.getId());
        dto.setTitle("New YouTube Video");
        dto.setMaterialType("EXTERNAL_LINK");
        dto.setResourceUrl("http://youtube.com/watch?v=123");
        dto.setDisplayOrder(2);

        mockMvc.perform(post("/api/v1/learning-materials/link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Thêm liên kết tài liệu thành công!"))
                .andExpect(jsonPath("$.data.title").value("New YouTube Video"));
    }

    @Test
    @WithMockUser(authorities = {"MATERIAL_UPDATE"})
    void testUpdateMaterialJson() throws Exception {
        LearningMaterialDto dto = new LearningMaterialDto();
        dto.setTitle("Updated Document Title");
        dto.setMaterialType("DOCUMENT");
        dto.setResourceUrl("http://example.com/updated-doc.pdf");

        mockMvc.perform(put("/api/v1/learning-materials/" + testMaterial.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật tài liệu học tập thành công!"))
                .andExpect(jsonPath("$.data.title").value("Updated Document Title"));
    }

    @Test
    @WithMockUser(authorities = {"MATERIAL_DELETE"})
    void testDeleteMaterial() throws Exception {
        mockMvc.perform(delete("/api/v1/learning-materials/" + testMaterial.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa tài liệu học tập thành công!"));
    }
}
