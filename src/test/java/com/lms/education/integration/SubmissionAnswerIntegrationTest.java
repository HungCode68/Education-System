package com.lms.education.integration;

import com.lms.education.module.lms.dto.SubmissionAnswerDto;
import com.lms.education.module.lms.entity.*;
import com.lms.education.module.lms.repository.*;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
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

public class SubmissionAnswerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SubmissionAnswerRepository submissionAnswerRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentQuestionRepository assignmentQuestionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private CourseRepository courseRepository;

    private Submission testSubmission;
    private Question testQuestion;
    private SubmissionAnswer testAnswer;
    private Lesson testLesson;
    private Classes testClass;

    @BeforeEach
    void setUp() {
        // Question
        List<Question> questions = questionRepository.findAll();
        if (questions.isEmpty()) {
            Question question = new Question();
            question.setQuestionType("ESSAY");
            question.setContent("Explain Spring Boot");
            testQuestion = questionRepository.save(question);
        } else {
            testQuestion = questions.get(0);
        }

        // Student
        List<Student> students = studentRepository.findAll();
        Student testStudent;
        if (students.isEmpty()) {
            Student student = new Student();
            student.setStudentCode("STD_ANS");
            student.setFullName("Answer Student");
            student.setStatus("STUDYING");
            testStudent = studentRepository.save(student);
        } else {
            testStudent = students.get(0);
        }

        // Course & Classes
        List<Course> courses = courseRepository.findAll();
        Course testCourse;
        if (courses.isEmpty()) {
            Course course = new Course();
            course.setCode("CRS_SUB_ANS");
            course.setName("Submission Answer Course");
            course.setBasePrice(new java.math.BigDecimal("1000000"));
            testCourse = courseRepository.save(course);
        } else {
            testCourse = courses.get(0);
        }

        List<Classes> classList = classesRepository.findAll();
        if (classList.isEmpty()) {
            Classes clazz = new Classes();
            clazz.setCode("CLS_SUB_ANS");
            clazz.setName("Submission Answer Class");
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
            lesson.setName("Answer Lesson");
            lesson.setOrderNumber(1);
            testLesson = lessonRepository.save(lesson);
        } else {
            testLesson = lessons.get(0);
        }

        // Assignment
        List<Assignment> assignments = assignmentRepository.findAll();
        Assignment testAssignment;
        if (assignments.isEmpty()) {
            Assignment assignment = new Assignment();
            assignment.setTitle("Answer Assignment");
            assignment.setAssignmentType("QUIZ");
            assignment.setStatus("PUBLISHED");
            assignment.setDueDate(java.time.LocalDateTime.now().plusDays(7));
            assignment.setLesson(testLesson);
            testAssignment = assignmentRepository.save(assignment);
        } else {
            testAssignment = assignments.get(0);
        }

        // AssignmentQuestion
        List<AssignmentQuestion> assignmentQuestions = assignmentQuestionRepository.findAll();
        if (assignmentQuestions.isEmpty()) {
            AssignmentQuestion aq = new AssignmentQuestion();
            aq.setId(new AssignmentQuestionId(testAssignment.getId(), testQuestion.getId()));
            aq.setAssignment(testAssignment);
            aq.setQuestion(testQuestion);
            aq.setScoreWeight(java.math.BigDecimal.ONE);
            aq.setOrderNumber(1);
            assignmentQuestionRepository.save(aq);
        }

        // Submission
        List<Submission> submissions = submissionRepository.findAll();
        if (submissions.isEmpty()) {
            Submission submission = new Submission();
            submission.setAssignment(testAssignment);
            submission.setStudent(testStudent);
            submission.setStatus("IN_PROGRESS");
            testSubmission = submissionRepository.save(submission);
        } else {
            testSubmission = submissions.get(0);
        }

        // Submission Answer
        List<SubmissionAnswer> answers = submissionAnswerRepository.findAll();
        if (answers.isEmpty()) {
            SubmissionAnswer answer = new SubmissionAnswer();
            answer.setSubmission(testSubmission);
            answer.setQuestion(testQuestion);
            answer.setTextAnswer("This is an existing answer");
            testAnswer = submissionAnswerRepository.save(answer);
        } else {
            testAnswer = answers.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"LMS_SUBMISSION_CREATE"})
    void testSaveOrUpdateAnswer() throws Exception {
        SubmissionAnswerDto dto = new SubmissionAnswerDto();
        dto.setQuestionId(testQuestion.getId());
        dto.setTextAnswer("This is a new text answer");

        mockMvc.perform(post("/api/v1/submission-answers/submission/" + testSubmission.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Lưu câu trả lời thành công!"))
                .andExpect(jsonPath("$.data.textAnswer").value("This is a new text answer"));
    }

    @Test
    @WithMockUser(authorities = {"LMS_SUBMISSION_UPDATE"})
    void testRemoveAnswer() throws Exception {
        mockMvc.perform(delete("/api/v1/submission-answers/submission/" + testSubmission.getId() + "/question/" + testQuestion.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa câu trả lời thành công!"));
    }

    @Test
    @WithMockUser(authorities = {"LMS_SUBMISSION_UPDATE"})
    void testGradeAnswer() throws Exception {
        mockMvc.perform(put("/api/v1/submission-answers/" + testAnswer.getId() + "/grade")
                .param("score", "8.5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Chấm điểm câu trả lời thành công!"));
    }
}
