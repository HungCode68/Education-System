package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.lms.dto.SubmissionDto;
import com.lms.education.module.lms.entity.Assignment;
import com.lms.education.module.lms.entity.Lesson;
import com.lms.education.module.lms.entity.Submission;
import com.lms.education.module.lms.repository.AssignmentRepository;
import com.lms.education.module.lms.repository.AssignmentQuestionRepository;
import com.lms.education.module.lms.repository.SubmissionRepository;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SubmissionServiceImplTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentQuestionRepository assignmentQuestionRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private ScheduleAssignmentRepository scheduleAssignmentRepository;
    @Mock
    private TeachingSubstitutionRepository teachingSubstitutionRepository;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    private Assignment mockAssignment;
    private Submission mockSubmission;
    private Student mockStudent;
    private User mockUser;
    private Staff mockStaff;
    private Lesson mockLesson;
    private Classes mockClass;
    
    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        mockClass = new Classes();
        mockClass.setId(10L);
        
        mockLesson = new Lesson();
        mockLesson.setId(1L);
        mockLesson.setClasses(mockClass);

        mockAssignment = new Assignment();
        mockAssignment.setId(100L);
        mockAssignment.setLesson(mockLesson);
        mockAssignment.setStatus("PUBLISHED");
        mockAssignment.setMaxAttempts(2);

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("student@test.com");

        mockStudent = new Student();
        mockStudent.setId(10L);
        mockStudent.setUser(mockUser);
        
        mockStaff = new Staff();
        mockStaff.setId(20L);

        mockSubmission = new Submission();
        mockSubmission.setId(1L);
        mockSubmission.setAssignment(mockAssignment);
        mockSubmission.setStudent(mockStudent);
        mockSubmission.setStatus("IN_PROGRESS");

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("student@test.com");
        
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(mockUser));
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.of(mockStudent));
    }
    
    @AfterEach
    void tearDown() {
        if (mockedSecurityContextHolder != null) {
            mockedSecurityContextHolder.close();
            mockedSecurityContextHolder = null;
        }
    }

    @Test
    void startSubmission_Success() {
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(mockAssignment));
        when(submissionRepository.countByAssignmentIdAndStudentId(100L, 10L)).thenReturn(0L);
        when(submissionRepository.findTopByAssignmentIdAndStudentIdOrderByStartTimeDesc(100L, 10L)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(Submission.class))).thenReturn(mockSubmission);

        SubmissionDto result = submissionService.startSubmission(100L);

        assertNotNull(result);
        assertEquals("IN_PROGRESS", result.getStatus());
    }

    @Test
    void startSubmission_Unpublished_ThrowsException() {
        mockAssignment.setStatus("UNPUBLISHED");
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(mockAssignment));

        assertThrows(OperationNotPermittedException.class, () -> submissionService.startSubmission(100L));
    }

    @Test
    void startSubmission_Closed_ThrowsException() {
        mockAssignment.setStatus("CLOSED");
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(mockAssignment));

        assertThrows(OperationNotPermittedException.class, () -> submissionService.startSubmission(100L));
    }

    @Test
    void startSubmission_MaxAttemptsReached_ThrowsException() {
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(mockAssignment));
        when(submissionRepository.countByAssignmentIdAndStudentId(100L, 10L)).thenReturn(2L);
        
        Submission gradedSubmission = new Submission();
        gradedSubmission.setStatus("GRADED");
        when(submissionRepository.findTopByAssignmentIdAndStudentIdOrderByStartTimeDesc(100L, 10L)).thenReturn(Optional.of(gradedSubmission));

        assertThrows(OperationNotPermittedException.class, () -> submissionService.startSubmission(100L));
    }

    @Test
    void submitAssignment_Success() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        when(assignmentQuestionRepository.findByAssignmentIdOrderByOrderNumberAsc(100L)).thenReturn(Collections.emptyList());
        when(submissionRepository.save(any(Submission.class))).thenReturn(mockSubmission);

        SubmissionDto result = submissionService.submitAssignment(1L);

        assertNotNull(result);
        verify(submissionRepository).save(mockSubmission);
    }
    
    @Test
    void submitAssignment_NotOwner_ThrowsException() {
        Student otherStudent = new Student();
        otherStudent.setId(99L);
        mockSubmission.setStudent(otherStudent);
        
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));

        assertThrows(OperationNotPermittedException.class, () -> submissionService.submitAssignment(1L));
    }

    @Test
    void submitAssignment_NotInProgress_ThrowsException() {
        mockSubmission.setStatus("SUBMITTED");
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));

        assertThrows(OperationNotPermittedException.class, () -> submissionService.submitAssignment(1L));
    }

    @Test
    void gradeSubmission_Success() {
        mockSubmission.setStatus("SUBMITTED");
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        
        // Setup teacher permission
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 20L)).thenReturn(true);
        
        when(submissionRepository.save(any(Submission.class))).thenReturn(mockSubmission);

        SubmissionDto result = submissionService.gradeSubmission(1L, new BigDecimal("8.5"), "Good job");

        assertNotNull(result);
        verify(submissionRepository).save(mockSubmission);
    }
    
    @Test
    void gradeSubmission_InProgress_ThrowsException() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        assertThrows(OperationNotPermittedException.class, () -> 
                submissionService.gradeSubmission(1L, new BigDecimal("8.5"), "Good job"));
    }
    
    @Test
    void gradeSubmission_TeacherNotAssigned_ThrowsException() {
        mockSubmission.setStatus("SUBMITTED");
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 20L)).thenReturn(false);
        when(teachingSubstitutionRepository.isTeacherSubstitutingForClass(10L, 1L, 20L)).thenReturn(false);

        assertThrows(OperationNotPermittedException.class, () -> 
                submissionService.gradeSubmission(1L, new BigDecimal("8.5"), "Good job"));
    }

    @Test
    void getById_Success() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));
        SubmissionDto result = submissionService.getById(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    void getMySubmission_Success() {
        when(submissionRepository.findTopByAssignmentIdAndStudentIdOrderByStartTimeDesc(100L, 10L)).thenReturn(Optional.of(mockSubmission));
        SubmissionDto result = submissionService.getMySubmission(100L);
        assertEquals(1L, result.getId());
    }

    @Test
    void getMySubmissionHistory_Success() {
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderByStartTimeDesc(100L, 10L)).thenReturn(List.of(mockSubmission));
        List<SubmissionDto> result = submissionService.getMySubmissionHistory(100L);
        assertEquals(1, result.size());
    }
    
    @Test
    void getByAssignmentId_Success() {
        when(submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(100L)).thenReturn(List.of(mockSubmission));
        List<SubmissionDto> result = submissionService.getByAssignmentId(100L);
        assertEquals(1, result.size());
    }
    
    @Test
    void getByStudentId_Success() {
        when(submissionRepository.findByStudentIdOrderBySubmittedAtDesc(10L)).thenReturn(List.of(mockSubmission));
        List<SubmissionDto> result = submissionService.getByStudentId(10L);
        assertEquals(1, result.size());
    }
    
    @Test
    void getMySubmissionsByClassId_Success() {
        when(submissionRepository.findByStudentIdOrderBySubmittedAtDesc(10L)).thenReturn(List.of(mockSubmission));
        List<SubmissionDto> result = submissionService.getMySubmissionsByClassId(10L);
        assertEquals(1, result.size());
    }
    
    @Test
    void getByAssignmentIdPageable_Success() {
        Page<Submission> page = new PageImpl<>(List.of(mockSubmission));
        Pageable pageable = PageRequest.of(0, 10);
        when(submissionRepository.findByAssignmentIdWithFilters(100L, "GRADED", "test", pageable)).thenReturn(page);
        
        Page<SubmissionDto> result = submissionService.getByAssignmentIdPageable(100L, "GRADED", "test", pageable);
        assertEquals(1, result.getTotalElements());
    }
}
