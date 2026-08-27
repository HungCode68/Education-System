package com.lms.education.integration;

import com.lms.education.module.user.dto.StudentDto;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class StudentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private StudentRepository studentRepository;

    private Student testStudent;

    @BeforeEach
    void setUp() {
        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) {
            Student student = new Student();
            student.setStudentCode("STD_TEST_01");
            student.setFullName("Test Student");
            student.setPhone("0987654321");
            student.setStatus("STUDYING");
            testStudent = studentRepository.save(student);
        } else {
            testStudent = students.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"STUDENT_VIEW"})
    void testGetStudentById() throws Exception {
        mockMvc.perform(get("/api/v1/students/" + testStudent.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testStudent.getId()));
    }

    @Test
    @WithMockUser(authorities = {"STUDENT_CREATE"})
    void testCreateStudent() throws Exception {
        StudentDto dto = new StudentDto();
        dto.setFullName("New Student");
        dto.setPhone("0912345678");

        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tạo hồ sơ học viên thành công!"))
                .andExpect(jsonPath("$.data.fullName").value("New Student"));
    }

    @Test
    @WithMockUser(authorities = {"STUDENT_UPDATE"})
    void testUpdateStudent() throws Exception {
        StudentDto dto = new StudentDto();
        dto.setFullName("Updated Student Name");
        dto.setPhone("0987654321");

        mockMvc.perform(put("/api/v1/students/" + testStudent.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật thông tin học viên thành công!"))
                .andExpect(jsonPath("$.data.fullName").value("Updated Student Name"));
    }

    @Test
    @WithMockUser(authorities = {"STUDENT_DELETE"})
    void testDeleteStudent() throws Exception {
        mockMvc.perform(delete("/api/v1/students/" + testStudent.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa hồ sơ học viên thành công!"));
    }
}
