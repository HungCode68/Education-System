package com.lms.education.integration;

import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.notification.dto.ClassAnnouncementDto;
import com.lms.education.module.notification.entity.ClassAnnouncement;
import com.lms.education.module.notification.repository.ClassAnnouncementRepository;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ClassAnnouncementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ClassAnnouncementRepository classAnnouncementRepository;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    private ClassAnnouncement testAnnouncement;
    private Classes testClass;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Course
        List<Course> courses = courseRepository.findAll();
        Course testCourse;
        if (courses.isEmpty()) {
            Course course = new Course();
            course.setCode("COURSE_ANN");
            course.setName("Announcement Course");
            course.setBasePrice(new java.math.BigDecimal("3000000"));
            testCourse = courseRepository.save(course);
        } else {
            testCourse = courses.get(0);
        }

        // Classes
        List<Classes> classList = classesRepository.findAll();
        if (classList.isEmpty()) {
            Classes clazz = new Classes();
            clazz.setCode("CLASS_ANN");
            clazz.setName("Announcement Class");
            clazz.setCourse(testCourse);
            clazz.setMaxStudents(30);
            clazz.setStatus("OPENING");
            testClass = classesRepository.save(clazz);
        } else {
            testClass = classList.get(0);
        }

        // User
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            User user = new User();
            user.setEmail("creator@example.com");
            user.setPassword("password");
            user.setFullName("Announcement Creator");
            user.setStatus("ACTIVE");
            testUser = userRepository.save(user);
        } else {
            testUser = users.get(0);
        }

        // Announcement
        List<ClassAnnouncement> announcements = classAnnouncementRepository.findAll();
        if (announcements.isEmpty()) {
            ClassAnnouncement announcement = new ClassAnnouncement();
            announcement.setClasses(testClass);
            announcement.setCreatedBy(testUser);
            announcement.setTitle("Test Announcement");
            announcement.setContent("This is a test announcement content");
            testAnnouncement = classAnnouncementRepository.save(announcement);
        } else {
            testAnnouncement = announcements.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"LMS_ANNOUNCEMENT_VIEW"})
    void testGetAnnouncementById() throws Exception {
        mockMvc.perform(get("/api/v1/class-announcements/" + testAnnouncement.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testAnnouncement.getId()));
    }

    @Test
    @WithMockUser(username = "creator@example.com", authorities = {"LMS_ANNOUNCEMENT_CREATE", "ADMIN"})
    void testCreateAnnouncement() throws Exception {
        ClassAnnouncementDto dto = new ClassAnnouncementDto();
        dto.setClassId(testClass.getId());
        dto.setTitle("New Announcement");
        dto.setContent("Content of the new announcement");

        mockMvc.perform(post("/api/v1/class-announcements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tạo thông báo lớp học thành công!"))
                .andExpect(jsonPath("$.data.title").value("New Announcement"));
    }

    @Test
    @WithMockUser(username = "creator@example.com", authorities = {"LMS_ANNOUNCEMENT_UPDATE", "ADMIN"})
    void testTogglePin() throws Exception {
        mockMvc.perform(patch("/api/v1/class-announcements/" + testAnnouncement.getId() + "/pin")
                .param("isPinned", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPinned").value(true));
    }

    @Test
    @WithMockUser(username = "creator@example.com", authorities = {"LMS_ANNOUNCEMENT_DELETE", "ADMIN"})
    void testDeleteAnnouncement() throws Exception {
        mockMvc.perform(delete("/api/v1/class-announcements/" + testAnnouncement.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa thông báo lớp học thành công!"));
    }
}
