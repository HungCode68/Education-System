package com.lms.education.integration;

import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.enrollment.dto.EnrollmentDto;
import com.lms.education.module.enrollment.entity.Enrollment;
import com.lms.education.module.enrollment.repository.EnrollmentRepository;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class EnrollmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    private Enrollment testEnrollment;
    private Classes testClass;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        // Prepare Course
        List<Course> courses = courseRepository.findAll();
        Course testCourse;
        if (courses.isEmpty()) {
            Course course = new Course();
            course.setCode("COURSE_ENROLL");
            course.setName("Enrollment Test Course");
            course.setBasePrice(new java.math.BigDecimal("1500000"));
            testCourse = courseRepository.save(course);
        } else {
            testCourse = courses.get(0);
        }

        // Prepare Classes
        List<Classes> classesList = classesRepository.findAll();
        if (classesList.isEmpty()) {
            Classes newClass = new Classes();
            newClass.setCode("CLASS_ENROLL");
            newClass.setName("Enrollment Test Class");
            newClass.setCourse(testCourse);
            newClass.setMaxStudents(30);
            newClass.setStatus("OPENING");
            testClass = classesRepository.save(newClass);
        } else {
            testClass = classesList.get(0);
        }

        // Prepare Student
        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) {
            Student student = new Student();
            student.setStudentCode("STU_ENROLL");
            student.setFullName("Test Student");
            student.setPhone("0999999999");
            student.setStatus("STUDYING");
            testStudent = studentRepository.save(student);
        } else {
            testStudent = students.get(0);
        }

        // Prepare Enrollment
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        if (enrollments.isEmpty()) {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(testStudent);
            enrollment.setClasses(testClass);
            enrollment.setEnrollmentDate(LocalDate.now());
            enrollment.setStatus("ACTIVE");
            testEnrollment = enrollmentRepository.save(enrollment);
        } else {
            testEnrollment = enrollments.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"ENROLLMENT_VIEW"})
    void testGetEnrollmentById() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/" + testEnrollment.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEnrollment.getId()));
    }

    @Test
    @WithMockUser(authorities = {"ENROLLMENT_CREATE"})
    void testCreateEnrollment() throws Exception {
        // Create another student to enroll
        Student student2 = new Student();
        student2.setStudentCode("STU_ENROLL_2");
        student2.setFullName("Test Student 2");
        student2.setPhone("0888888888");
        student2.setStatus("STUDYING");
        Student testStudent2 = studentRepository.save(student2);

        EnrollmentDto newEnrollment = new EnrollmentDto();
        newEnrollment.setStudentId(testStudent2.getId());
        newEnrollment.setClassId(testClass.getId());
        newEnrollment.setEnrollmentDate(LocalDate.now());
        newEnrollment.setStatus("ACTIVE");

        mockMvc.perform(post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newEnrollment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Đăng ký học viên vào lớp thành công!"))
                .andExpect(jsonPath("$.data.studentId").value(testStudent2.getId()));
    }

    @Test
    @WithMockUser(authorities = {"ENROLLMENT_UPDATE"})
    void testUpdateEnrollment() throws Exception {
        EnrollmentDto updateDto = new EnrollmentDto();
        updateDto.setStudentId(testStudent.getId());
        updateDto.setClassId(testClass.getId());
        updateDto.setEnrollmentDate(LocalDate.now());
        updateDto.setStatus("DROPPED");
        updateDto.setNote("Student dropped");

        mockMvc.perform(put("/api/v1/enrollments/" + testEnrollment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật đăng ký thành công!"))
                .andExpect(jsonPath("$.data.status").value("DROPPED"))
                .andExpect(jsonPath("$.data.note").value("Student dropped"));
    }

    @Test
    @WithMockUser(authorities = {"ENROLLMENT_DELETE"})
    void testDeleteEnrollment() throws Exception {
        mockMvc.perform(delete("/api/v1/enrollments/" + testEnrollment.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa đăng ký học viên thành công!"));
    }
}
