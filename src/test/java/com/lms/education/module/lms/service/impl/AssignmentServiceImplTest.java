package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.lms.dto.AssignmentDto;
import com.lms.education.module.lms.entity.Assignment;
import com.lms.education.module.lms.entity.Lesson;
import com.lms.education.module.lms.repository.AssignmentRepository;
import com.lms.education.module.lms.repository.LessonRepository;
import com.lms.education.module.lms.repository.SubmissionRepository;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StaffRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AssignmentServiceImplTest {

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private ScheduleAssignmentRepository scheduleAssignmentRepository;
    @Mock
    private TeachingSubstitutionRepository teachingSubstitutionRepository;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    private Assignment mockAssignment;
    private AssignmentDto mockDto;
    private Lesson mockLesson;
    private Classes mockClass;
    private User mockUser;
    private Staff mockStaff;
    
    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        mockClass = new Classes();
        mockClass.setId(10L);

        mockLesson = new Lesson();
        mockLesson.setId(1L);
        mockLesson.setClasses(mockClass);

        mockUser = new User();
        mockUser.setId(100L);
        mockUser.setEmail("teacher@test.com");
        
        mockStaff = new Staff();
        mockStaff.setId(200L);

        mockAssignment = new Assignment();
        mockAssignment.setId(1L);
        mockAssignment.setLesson(mockLesson);
        mockAssignment.setTitle("Assignment 1");
        mockAssignment.setAssignmentType("HOMEWORK");
        mockAssignment.setStatus("PUBLISHED");

        mockDto = AssignmentDto.builder()
                .lessonId(1L)
                .title("Assignment 1")
                .dueDate(LocalDateTime.now().plusDays(1))
                .status("PUBLISHED")
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("teacher@test.com");
        
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }
    
    @AfterEach
    void tearDown() {
        if (mockedSecurityContextHolder != null) {
            mockedSecurityContextHolder.close();
            mockedSecurityContextHolder = null;
        }
    }

    private void setupTeacherPermission() {
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(100L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 100L, 200L)).thenReturn(true);
    }

    @Test
    void create_Success() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(mockAssignment);

        AssignmentDto result = assignmentService.create(mockDto);

        assertNotNull(result);
        assertEquals("Assignment 1", result.getTitle());
    }

    @Test
    void create_LessonNotFound_ThrowsException() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> assignmentService.create(mockDto));
    }

    @Test
    void create_InvalidDueDate_ThrowsException() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        mockDto.setDueDate(LocalDateTime.now().minusDays(1)); // Past date
        assertThrows(OperationNotPermittedException.class, () -> assignmentService.create(mockDto));
    }
    
    @Test
    void create_InvalidTimeLimit_ThrowsException() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        mockDto.setTimeLimitMinutes(-10);
        assertThrows(OperationNotPermittedException.class, () -> assignmentService.create(mockDto));
    }
    
    @Test
    void create_InvalidMaxAttempts_ThrowsException() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        mockDto.setMaxAttempts(0);
        assertThrows(OperationNotPermittedException.class, () -> assignmentService.create(mockDto));
    }

    @Test
    void update_Success() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(mockAssignment);

        AssignmentDto result = assignmentService.update(1L, mockDto);

        assertNotNull(result);
    }

    @Test
    void update_CannotUnpublish_ThrowsException() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment)); // STATUS is PUBLISHED
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(submissionRepository.existsByAssignmentId(1L)).thenReturn(true);
        
        mockDto.setStatus("UNPUBLISHED");

        assertThrows(OperationNotPermittedException.class, () -> assignmentService.update(1L, mockDto));
    }

    @Test
    void delete_Success() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        setupTeacherPermission();
        when(submissionRepository.existsByAssignmentId(1L)).thenReturn(false);

        assignmentService.delete(1L);

        verify(assignmentRepository).delete(mockAssignment);
    }
    
    @Test
    void delete_HasSubmissions_ThrowsException() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        setupTeacherPermission();
        when(submissionRepository.existsByAssignmentId(1L)).thenReturn(true);

        assertThrows(OperationNotPermittedException.class, () -> assignmentService.delete(1L));
    }

    @Test
    void getById_Success() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        setupTeacherPermission(); // canViewDraftAssignment
        AssignmentDto result = assignmentService.getById(1L);
        assertEquals("Assignment 1", result.getTitle());
    }

    @Test
    void getByLessonId_Success() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission(); // canViewDraftAssignment
        when(assignmentRepository.findByLessonIdOrderByDueDateAsc(1L)).thenReturn(List.of(mockAssignment));
        List<AssignmentDto> result = assignmentService.getByLessonId(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getByClassId_Success() {
        setupTeacherPermission(); // canViewDraftAssignment
        when(assignmentRepository.findByLessonClassesIdOrderByDueDateAsc(10L)).thenReturn(List.of(mockAssignment));
        List<AssignmentDto> result = assignmentService.getByClassId(10L);
        assertEquals(1, result.size());
    }

    @Test
    void getAll_Success() {
        Page<Assignment> page = new PageImpl<>(List.of(mockAssignment));
        Pageable pageable = PageRequest.of(0, 10);
        when(assignmentRepository.searchAssignments("Assignment", pageable)).thenReturn(page);
        
        // mockUser is teacher, isStaffOrTeacherUser will be true if roles are loaded. Let's just mock user without roles.
        // Actually the mockUser currently doesn't have roles. The isStaffOrTeacherUser returns false.
        // So the filtering logic will be applied, but "PUBLISHED" is allowed.
        
        Page<AssignmentDto> result = assignmentService.getAll("Assignment", pageable);

        assertEquals(1, result.getTotalElements());
    }
    
    @Test
    void autoCloseExpiredAssignments() {
        when(assignmentRepository.closeExpiredAssignments(any())).thenReturn(5);
        assignmentService.autoCloseExpiredAssignments();
        verify(assignmentRepository).closeExpiredAssignments(any());
    }

    @Test
    void create_PermissionDenied_ThrowsException() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(100L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 100L, 200L)).thenReturn(false);
        when(teachingSubstitutionRepository.isTeacherSubstitutingForClass(10L, 100L, 200L)).thenReturn(false);

        assertThrows(OperationNotPermittedException.class, () -> assignmentService.create(mockDto));
    }

    @Test
    void update_InvalidFields_ThrowsException() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();

        mockDto.setDueDate(LocalDateTime.now().minusDays(1));
        assertThrows(OperationNotPermittedException.class, () -> assignmentService.update(1L, mockDto));
        
        mockDto.setDueDate(null);
        mockDto.setTimeLimitMinutes(-5);
        assertThrows(OperationNotPermittedException.class, () -> assignmentService.update(1L, mockDto));
        
        mockDto.setTimeLimitMinutes(null);
        mockDto.setMaxAttempts(0);
        assertThrows(OperationNotPermittedException.class, () -> assignmentService.update(1L, mockDto));
    }
    
    @Test
    void update_Unpublish_Success() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment)); // PUBLISHED
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(submissionRepository.existsByAssignmentId(1L)).thenReturn(false);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(i -> i.getArgument(0));
        
        mockDto.setStatus("UNPUBLISHED");

        AssignmentDto result = assignmentService.update(1L, mockDto);
        assertEquals("UNPUBLISHED", result.getStatus());
    }

    @Test
    void getAll_Success_EmptyKeyword() {
        Page<Assignment> page = new PageImpl<>(List.of(mockAssignment));
        Pageable pageable = PageRequest.of(0, 10);
        when(assignmentRepository.findAll(pageable)).thenReturn(page);
        
        Page<AssignmentDto> result = assignmentService.getAll("", pageable);

        assertEquals(1, result.getTotalElements());
    }

    // --- Extra branch coverage tests ---

    @Test
    void create_WithNullOptionalFields() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(i -> i.getArgument(0));

        mockDto.setDueDate(null);
        mockDto.setAssignmentType(null);
        mockDto.setTimeLimitMinutes(null);
        mockDto.setMaxAttempts(null);
        mockDto.setShowCorrectAnswers(null);
        mockDto.setStatus(null);
        
        AssignmentDto result = assignmentService.create(mockDto);

        assertNotNull(result);
        assertEquals("HOMEWORK", result.getAssignmentType());
        assertEquals(0, result.getTimeLimitMinutes());
        assertEquals(1, result.getMaxAttempts());
        assertTrue(result.getShowCorrectAnswers());
        assertEquals("PUBLISHED", result.getStatus());
    }

    @Test
    void update_WithNullFields_Success() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(i -> i.getArgument(0));

        mockDto.setLessonId(null);
        mockDto.setTitle(null);
        mockDto.setDescription(null);
        mockDto.setDueDate(null);
        mockDto.setAssignmentType(null);
        mockDto.setTimeLimitMinutes(null);
        mockDto.setMaxAttempts(null);
        mockDto.setShowCorrectAnswers(null);
        mockDto.setStatus(null);

        AssignmentDto result = assignmentService.update(1L, mockDto);

        assertNotNull(result);
    }

    @Test
    void update_Draft_Success() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        mockAssignment.setStatus("CLOSED");
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(submissionRepository.existsByAssignmentId(1L)).thenReturn(false);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(i -> i.getArgument(0));
        
        mockDto.setStatus("DRAFT");
        AssignmentDto result = assignmentService.update(1L, mockDto);
        assertEquals("DRAFT", result.getStatus());
    }

    @Test
    void getById_Unpublished_NoPermission_ThrowsException() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        mockAssignment.setStatus("UNPUBLISHED");
        
        // Mock a user with no permission (e.g. a student)
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(100L)).thenReturn(Optional.empty()); // Not a staff
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 100L, null)).thenReturn(false);
        when(teachingSubstitutionRepository.isTeacherSubstitutingForClass(10L, 100L, null)).thenReturn(false);
        
        assertThrows(ResourceNotFoundException.class, () -> assignmentService.getById(1L));
    }

    @Test
    void getByLessonId_Unpublished_Filtered() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        
        // Mock a student user
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(100L)).thenReturn(Optional.empty());
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 100L, null)).thenReturn(false);
        when(teachingSubstitutionRepository.isTeacherSubstitutingForClass(10L, 100L, null)).thenReturn(false);
        
        Assignment publishedAssignment = new Assignment();
        publishedAssignment.setStatus("PUBLISHED");
        publishedAssignment.setLesson(mockLesson);
        Assignment unpublishedAssignment = new Assignment();
        unpublishedAssignment.setStatus("UNPUBLISHED");
        unpublishedAssignment.setLesson(mockLesson);

        when(assignmentRepository.findByLessonIdOrderByDueDateAsc(1L))
                .thenReturn(List.of(publishedAssignment, unpublishedAssignment));
        
        List<AssignmentDto> result = assignmentService.getByLessonId(1L);
        assertEquals(1, result.size());
        assertEquals("PUBLISHED", result.get(0).getStatus());
    }

    @Test
    void getByClassId_Unpublished_Filtered() {
        // Mock a student user
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(100L)).thenReturn(Optional.empty());
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 100L, null)).thenReturn(false);
        when(teachingSubstitutionRepository.isTeacherSubstitutingForClass(10L, 100L, null)).thenReturn(false);
        
        Assignment publishedAssignment = new Assignment();
        publishedAssignment.setStatus("PUBLISHED");
        publishedAssignment.setLesson(mockLesson);
        Assignment unpublishedAssignment = new Assignment();
        unpublishedAssignment.setStatus("UNPUBLISHED");
        unpublishedAssignment.setLesson(mockLesson);

        when(assignmentRepository.findByLessonClassesIdOrderByDueDateAsc(10L))
                .thenReturn(List.of(publishedAssignment, unpublishedAssignment));
        
        List<AssignmentDto> result = assignmentService.getByClassId(10L);
        assertEquals(1, result.size());
        assertEquals("PUBLISHED", result.get(0).getStatus());
    }

    @Test
    void getAll_Unpublished_Filtered_ForStudent() {
        // isStaffOrTeacherUser = false
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(100L)).thenReturn(Optional.empty());
        // Setup mockUser with student role
        com.lms.education.module.user.entity.Role studentRole = new com.lms.education.module.user.entity.Role();
        studentRole.setName("ROLE_STUDENT");
        mockUser.setRoles(java.util.Set.of(studentRole));
        
        Assignment publishedAssignment = new Assignment();
        publishedAssignment.setStatus("PUBLISHED");
        publishedAssignment.setLesson(mockLesson);
        Assignment draftAssignment = new Assignment();
        draftAssignment.setStatus("DRAFT");
        draftAssignment.setLesson(mockLesson);
        
        Page<Assignment> page = new PageImpl<>(List.of(publishedAssignment, draftAssignment));
        Pageable pageable = PageRequest.of(0, 10);
        when(assignmentRepository.findAll(pageable)).thenReturn(page);
        
        Page<AssignmentDto> result = assignmentService.getAll("", pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals("PUBLISHED", result.getContent().get(0).getStatus());
    }

    // --- More Edge Cases for Branch Coverage ---

    @Test
    void create_WithEmptyStringFields_FallsBackToDefaults() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(i -> i.getArgument(0));

        mockDto.setAssignmentType("   "); // empty after trim
        mockDto.setStatus("   "); // empty after trim

        AssignmentDto result = assignmentService.create(mockDto);

        assertNotNull(result);
        assertEquals("HOMEWORK", result.getAssignmentType());
        assertEquals("PUBLISHED", result.getStatus());
    }
    
    @Test
    void update_WithEmptyStringFields_IgnoresUpdate() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(i -> i.getArgument(0));

        mockDto.setAssignmentType("   "); // empty after trim
        mockDto.setStatus("   "); // empty after trim

        AssignmentDto result = assignmentService.update(1L, mockDto);

        assertNotNull(result);
        assertEquals("HOMEWORK", result.getAssignmentType()); // remains unchanged
        assertEquals("PUBLISHED", result.getStatus()); // remains unchanged
    }
    
    @Test
    void getById_WithAdminRole_CanViewDraft() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        mockAssignment.setStatus("UNPUBLISHED");

        com.lms.education.module.user.entity.Role adminRole = new com.lms.education.module.user.entity.Role();
        adminRole.setName("ROLE_ADMIN");
        mockUser.setRoles(java.util.Set.of(adminRole));
        
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        
        AssignmentDto result = assignmentService.getById(1L);
        assertEquals("UNPUBLISHED", result.getStatus());
    }

    @Test
    void getById_Unauthenticated_CannotViewDraft() {
        SecurityContextHolder.getContext().setAuthentication(null); // anonymous
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        mockAssignment.setStatus("DRAFT");
        
        assertThrows(ResourceNotFoundException.class, () -> assignmentService.getById(1L));
    }
    
    @Test
    void getByLessonId_NullClassId_Filtered() {
        // Return empty so lesson is null, and classId becomes null
        when(lessonRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        
        Assignment draftAssignment = new Assignment();
        draftAssignment.setStatus("DRAFT");
        draftAssignment.setLesson(mockLesson); // Use mockLesson so mapToDto doesn't NPE if it gets called

        when(assignmentRepository.findByLessonIdOrderByDueDateAsc(1L)).thenReturn(List.of(draftAssignment));
        
        List<AssignmentDto> result = assignmentService.getByLessonId(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAll_IsStaffOrTeacher_AllRoles() {
        Page<Assignment> page = new PageImpl<>(List.of(mockAssignment));
        Pageable pageable = PageRequest.of(0, 10);
        when(assignmentRepository.findAll(pageable)).thenReturn(page);
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));

        String[] roles = {"ROLE_MANAGER", "ROLE_ACADEMIC", "ROLE_TRAINING"};
        for (String roleName : roles) {
            com.lms.education.module.user.entity.Role role = new com.lms.education.module.user.entity.Role();
            role.setName(roleName);
            mockUser.setRoles(java.util.Set.of(role));

            Page<AssignmentDto> result = assignmentService.getAll(null, pageable);
            assertEquals(1, result.getTotalElements());
        }
    }
}
