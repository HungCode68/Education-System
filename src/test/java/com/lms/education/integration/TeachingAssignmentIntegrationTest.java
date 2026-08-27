package com.lms.education.integration;

import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.teaching.dto.TeachingAssignmentDto;
import com.lms.education.module.teaching.entity.TeachingAssignment;
import com.lms.education.module.teaching.repository.TeachingAssignmentRepository;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TeachingAssignmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TeachingAssignmentRepository teachingAssignmentRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private CourseRepository courseRepository;

    private TeachingAssignment testAssignment;
    private Staff testStaff;
    private Classes testClass;

    @BeforeEach
    void setUp() {
        // Staff
        List<Staff> staffs = staffRepository.findAll();
        if (staffs.isEmpty()) {
            Staff staff = new Staff();
            staff.setStaffCode("STF_TEACH");
            staff.setStaffType("TEACHER");
            staff.setFullName("Test Teacher");
            staff.setStatus("ACTIVE");
            testStaff = staffRepository.save(staff);
        } else {
            testStaff = staffs.get(0);
        }

        // Course
        List<Course> courses = courseRepository.findAll();
        Course testCourse;
        if (courses.isEmpty()) {
            Course course = new Course();
            course.setCode("COURSE_TA");
            course.setName("Teaching Assignment Course");
            course.setBasePrice(new java.math.BigDecimal("1500000"));
            testCourse = courseRepository.save(course);
        } else {
            testCourse = courses.get(0);
        }

        // Classes
        List<Classes> classList = classesRepository.findAll();
        if (classList.isEmpty()) {
            Classes clazz = new Classes();
            clazz.setCode("CLASS_TA");
            clazz.setName("Teaching Assignment Class");
            clazz.setCourse(testCourse);
            clazz.setMaxStudents(30);
            clazz.setStatus("OPENING");
            testClass = classesRepository.save(clazz);
        } else {
            testClass = classList.get(0);
        }

        // Teaching Assignment
        List<TeachingAssignment> assignments = teachingAssignmentRepository.findAll();
        if (assignments.isEmpty()) {
            TeachingAssignment assignment = new TeachingAssignment();
            assignment.setTeacher(testStaff);
            assignment.setClasses(testClass);
            assignment.setRole("MAIN_TEACHER");
            assignment.setAssignedDate(LocalDate.now());
            assignment.setStatus("ACTIVE");
            testAssignment = teachingAssignmentRepository.save(assignment);
        } else {
            testAssignment = assignments.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"ASSIGNMENT_VIEW"})
    void testGetAssignmentById() throws Exception {
        mockMvc.perform(get("/api/v1/teaching-assignments/" + testAssignment.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testAssignment.getId()));
    }

    @Test
    @WithMockUser(authorities = {"ASSIGNMENT_CREATE"})
    void testCreateAssignment() throws Exception {
        Staff newStaff = new Staff();
        newStaff.setStaffCode("STF_TA_NEW");
        newStaff.setStaffType("TEACHER");
        newStaff.setFullName("New Teaching Teacher");
        newStaff.setStatus("ACTIVE");
        newStaff = staffRepository.save(newStaff);

        TeachingAssignmentDto dto = new TeachingAssignmentDto();
        dto.setClassId(testClass.getId());
        dto.setStaffId(newStaff.getId());
        dto.setRole("ASSISTANT_TEACHER");
        dto.setAssignedDate(LocalDate.now());
        dto.setStatus("ACTIVE");

        mockMvc.perform(post("/api/v1/teaching-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Phân công giảng dạy thành công!"))
                .andExpect(jsonPath("$.data.role").value("ASSISTANT_TEACHER"));
    }

    @Test
    @WithMockUser(authorities = {"ASSIGNMENT_UPDATE"})
    void testUpdateAssignment() throws Exception {
        TeachingAssignmentDto updateDto = new TeachingAssignmentDto();
        updateDto.setStaffId(testStaff.getId());
        updateDto.setClassId(testClass.getId());
        updateDto.setRole("TUTOR");
        updateDto.setAssignedDate(LocalDate.now());
        updateDto.setStatus("INACTIVE");

        mockMvc.perform(put("/api/v1/teaching-assignments/" + testAssignment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật phân công giảng dạy thành công!"))
                .andExpect(jsonPath("$.data.role").value("TUTOR"));
    }

    @Test
    @WithMockUser(authorities = {"ASSIGNMENT_DELETE"})
    void testDeleteAssignment() throws Exception {
        mockMvc.perform(delete("/api/v1/teaching-assignments/" + testAssignment.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa phân công giảng dạy thành công!"));
    }
}
