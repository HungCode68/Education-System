package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.lms.dto.LessonDto;
import com.lms.education.module.lms.entity.Lesson;
import com.lms.education.module.lms.repository.AssignmentRepository;
import com.lms.education.module.lms.repository.LearningMaterialRepository;
import com.lms.education.module.lms.repository.LessonRepository;
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
import org.mockito.Mockito;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LessonServiceImplTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private ClassesRepository classesRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private ScheduleAssignmentRepository scheduleAssignmentRepository;
    @Mock
    private TeachingSubstitutionRepository teachingSubstitutionRepository;
    @Mock
    private LearningMaterialRepository learningMaterialRepository;
    @Mock
    private AssignmentRepository assignmentRepository;

    @InjectMocks
    private LessonServiceImpl lessonService;

    private Lesson mockLesson;
    private LessonDto mockDto;
    private Classes mockClass;
    private User mockUser;
    private Staff mockStaff;
    
    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        mockClass = new Classes();
        mockClass.setId(10L);
        mockClass.setCode("C01");

        mockLesson = new Lesson();
        mockLesson.setId(1L);
        mockLesson.setClasses(mockClass);
        mockLesson.setName("Lesson 1");
        mockLesson.setOrderNumber(1);

        mockDto = LessonDto.builder()
                .classId(10L)
                .name("Lesson 1")
                .orderNumber(1)
                .build();
                
        mockUser = new User();
        mockUser.setId(100L);
        mockUser.setEmail("teacher@test.com");
        
        mockStaff = new Staff();
        mockStaff.setId(200L);
        
        // Mock SecurityContextHolder
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
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        setupTeacherPermission();
        when(lessonRepository.existsByClassesIdAndOrderNumber(10L, 1)).thenReturn(false);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(mockLesson);

        LessonDto result = lessonService.create(mockDto);

        assertNotNull(result);
        assertEquals("Lesson 1", result.getName());
    }

    @Test
    void create_ClassNotFound_ThrowsException() {
        when(classesRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> lessonService.create(mockDto));
    }

    @Test
    void create_TeacherNotAssigned_ThrowsException() {
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(100L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 100L, 200L)).thenReturn(false);
        when(teachingSubstitutionRepository.isTeacherSubstitutingForClass(10L, 100L, 200L)).thenReturn(false);

        assertThrows(OperationNotPermittedException.class, () -> lessonService.create(mockDto));
    }

    @Test
    void create_DuplicateOrder_ThrowsException() {
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        setupTeacherPermission();
        when(lessonRepository.existsByClassesIdAndOrderNumber(10L, 1)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> lessonService.create(mockDto));
    }

    @Test
    void update_Success() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        setupTeacherPermission();
        when(lessonRepository.save(any(Lesson.class))).thenReturn(mockLesson);

        LessonDto result = lessonService.update(1L, mockDto);

        assertNotNull(result);
        verify(lessonRepository).save(any(Lesson.class));
    }
    
    @Test
    void update_DuplicateOrder_ThrowsException() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        setupTeacherPermission();
        
        mockDto.setOrderNumber(2);
        when(lessonRepository.existsByClassesIdAndOrderNumber(10L, 2)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> lessonService.update(1L, mockDto));
    }

    @Test
    void update_LessonNotFound_ThrowsException() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> lessonService.update(1L, mockDto));
    }

    @Test
    void delete_Success() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(learningMaterialRepository.existsByLessonId(1L)).thenReturn(false);
        when(assignmentRepository.existsByLessonId(1L)).thenReturn(false);

        lessonService.delete(1L);

        verify(lessonRepository).delete(mockLesson);
    }
    
    @Test
    void delete_HasMaterials_ThrowsException() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(learningMaterialRepository.existsByLessonId(1L)).thenReturn(true);

        assertThrows(OperationNotPermittedException.class, () -> lessonService.delete(1L));
    }

    @Test
    void delete_LessonNotFound_ThrowsException() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> lessonService.delete(1L));
    }

    @Test
    void getById_Success() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        LessonDto result = lessonService.getById(1L);
        assertEquals("Lesson 1", result.getName());
    }

    @Test
    void getAll_Success() {
        Page<Lesson> page = new PageImpl<>(List.of(mockLesson));
        Pageable pageable = PageRequest.of(0, 10);
        when(lessonRepository.searchLessons("Lesson", pageable)).thenReturn(page);

        Page<LessonDto> result = lessonService.getAll("Lesson", pageable);

        assertEquals(1, result.getTotalElements());
    }
    
    @Test
    void getByClassId_Success() {
        when(lessonRepository.findByClassesIdOrderByOrderNumberAsc(10L)).thenReturn(List.of(mockLesson));
        
        List<LessonDto> result = lessonService.getByClassId(10L);
        
        assertEquals(1, result.size());
    }
}
