package com.lms.education.integration;

import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.lms.entity.Assignment;
import com.lms.education.module.lms.entity.Lesson;
import com.lms.education.module.lms.entity.Submission;
import com.lms.education.module.lms.repository.AssignmentRepository;
import com.lms.education.module.lms.repository.LessonRepository;
import com.lms.education.module.lms.repository.SubmissionRepository;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class SubmissionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    private Assignment testAssignment;
    private Student testStudent;
    private Submission testSubmission;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Prepare User & Student
        Optional<User> userOpt = userRepository.findByEmail("student@test.com");
        if (userOpt.isEmpty()) {
            User user = new User();
            user.setEmail("student@test.com");
            user.setPassword("password");
            user.setFullName("Test Student Sub");
            user.setStatus("ACTIVE");
            testUser = userRepository.save(user);
        } else {
            testUser = userOpt.get();
        }

        Optional<Student> studentOpt = studentRepository.findByUserId(testUser.getId());
        if (studentOpt.isEmpty()) {
            Student student = new Student();
            student.setUser(testUser);
            student.setStudentCode("STU_SUB");
            student.setFullName(testUser.getFullName());
            student.setPhone("0999999999");
            student.setStatus("STUDYING");
            testStudent = studentRepository.save(student);
        } else {
            testStudent = studentOpt.get();
        }

        // Prepare Course
        List<Course> courses = courseRepository.findAll();
        Course testCourse;
        if (courses.isEmpty()) {
            Course course = new Course();
            course.setCode("COURSE_SUB");
            course.setName("Submission Test Course");
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
            newClass.setCode("CLASS_SUB");
            newClass.setName("Submission Test Class");
            newClass.setCourse(testCourse);
            newClass.setMaxStudents(30);
            newClass.setStatus("OPENING");
            testClass = classesRepository.save(newClass);
        } else {
            testClass = classesList.get(0);
        }

        // Prepare Lesson
        List<Lesson> lessons = lessonRepository.findAll();
        Lesson testLesson;
        if (lessons.isEmpty()) {
            Lesson lesson = new Lesson();
            lesson.setClasses(testClass);
            lesson.setName("Test Lesson for Submission");
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
            assignment.setTitle("Test Assignment for Submission");
            assignment.setDueDate(LocalDateTime.now().plusDays(7));
            assignment.setAssignmentType("HOMEWORK");
            assignment.setStatus("PUBLISHED");
            assignment.setMaxAttempts(3);
            testAssignment = assignmentRepository.save(assignment);
        } else {
            testAssignment = assignments.get(0);
        }

        // Prepare Submission
        List<Submission> submissions = submissionRepository.findAll();
        if (submissions.isEmpty()) {
            Submission submission = new Submission();
            submission.setAssignment(testAssignment);
            submission.setStudent(testStudent);
            submission.setStartTime(LocalDateTime.now().minusMinutes(30));
            submission.setStatus("IN_PROGRESS");
            testSubmission = submissionRepository.save(submission);
        } else {
            testSubmission = submissions.get(0);
        }
    }

    @Test
    @WithMockUser(username = "student@test.com", authorities = {"LMS_SUBMISSION_START"})
    void testStartSubmission() throws Exception {
        // Need a new assignment to start (since testAssignment already has an IN_PROGRESS submission)
        Assignment newAssignment = new Assignment();
        newAssignment.setLesson(testAssignment.getLesson());
        newAssignment.setTitle("Another Assignment");
        newAssignment.setDueDate(LocalDateTime.now().plusDays(5));
        newAssignment.setAssignmentType("QUIZ");
        newAssignment.setStatus("PUBLISHED");
        newAssignment.setMaxAttempts(3);
        Assignment savedAssignment = assignmentRepository.save(newAssignment);

        mockMvc.perform(post("/api/v1/submissions/start/" + savedAssignment.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Bắt đầu làm bài tập thành công!"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(username = "student@test.com", authorities = {"LMS_SUBMISSION_SUBMIT"})
    void testSubmitAssignment() throws Exception {
        mockMvc.perform(post("/api/v1/submissions/submit/" + testSubmission.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Nộp bài tập thành công!"))
                .andExpect(jsonPath("$.data.status").exists()); // GRADED, SUBMITTED or LATE
    }
}
