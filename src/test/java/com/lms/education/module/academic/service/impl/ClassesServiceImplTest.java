package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.ClassesDto;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.entity.Term;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.academic.repository.ScheduleCancellationRepository;
import com.lms.education.module.academic.repository.TermRepository;
import com.lms.education.module.enrollment.repository.EnrollmentRepository;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingAssignmentRepository;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClassesServiceImplTest {

    @Mock
    private ClassesRepository classesRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TermRepository termRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private ScheduleAssignmentRepository scheduleAssignmentRepository;
    @Mock
    private TeachingAssignmentRepository teachingAssignmentRepository;
    @Mock
    private ClassScheduleRepository classScheduleRepository;
    @Mock
    private ScheduleCancellationRepository scheduleCancellationRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private ClassesServiceImpl classesService;

    private Classes mockClass;
    private ClassesDto mockClassDto;
    private Course mockCourse;
    private Term mockTerm;
    
    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        mockCourse = new Course();
        mockCourse.setId(10L);
        mockCourse.setCode("ENG101");
        mockCourse.setTotalSessions(20);
        mockCourse.setSessionsPerWeek(2);

        mockTerm = new Term();
        mockTerm.setId(20L);
        mockTerm.setCode("SP2024");

        mockClass = new Classes();
        mockClass.setId(1L);
        mockClass.setCode("C01");
        mockClass.setName("Class 01");
        mockClass.setCourse(mockCourse);
        mockClass.setTerm(mockTerm);
        mockClass.setStartDate(LocalDate.of(2024, 1, 1));
        mockClass.setEndDate(LocalDate.of(2024, 6, 1));
        mockClass.setMaxStudents(30);

        mockClassDto = ClassesDto.builder()
                .code("c01")
                .name("Class 01")
                .courseId(10L)
                .termId(20L)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 6, 1))
                .maxStudents(30)
                .build();
    }
    
    @AfterEach
    void tearDown() {
        if (mockedSecurityContextHolder != null) {
            mockedSecurityContextHolder.close();
            mockedSecurityContextHolder = null;
        }
    }

    @Test
    void create_Success() {
        when(classesRepository.existsByCode("C01")).thenReturn(false);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(mockCourse));
        when(termRepository.findById(20L)).thenReturn(Optional.of(mockTerm));
        when(classesRepository.save(any(Classes.class))).thenReturn(mockClass);
        ClassesDto result = classesService.create(mockClassDto);

        assertNotNull(result);
        assertEquals("C01", result.getCode());
        verify(classesRepository).save(any(Classes.class));
    }

    @Test
    void create_DuplicateCode_ThrowsException() {
        when(classesRepository.existsByCode("C01")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> classesService.create(mockClassDto));
    }

    @Test
    void create_InvalidDates_ThrowsException() {
        mockClassDto.setStartDate(LocalDate.of(2024, 7, 1));
        when(classesRepository.existsByCode("C01")).thenReturn(false);
        assertThrows(OperationNotPermittedException.class, () -> classesService.create(mockClassDto));
    }

    @Test
    void create_CourseNotFound_ThrowsException() {
        when(classesRepository.existsByCode("C01")).thenReturn(false);
        when(courseRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> classesService.create(mockClassDto));
    }

    @Test
    void update_Success() {
        when(classesRepository.findById(1L)).thenReturn(Optional.of(mockClass));
        mockClassDto.setCode("C02");
        when(classesRepository.existsByCode("C02")).thenReturn(false);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(mockCourse));
        when(termRepository.findById(20L)).thenReturn(Optional.of(mockTerm));
        when(classesRepository.save(any(Classes.class))).thenReturn(mockClass);

        ClassesDto result = classesService.update(1L, mockClassDto);

        assertNotNull(result);
        verify(classesRepository).save(any(Classes.class));
    }

    @Test
    void update_DuplicateCode_ThrowsException() {
        when(classesRepository.findById(1L)).thenReturn(Optional.of(mockClass));
        mockClassDto.setCode("C02");
        when(classesRepository.existsByCode("C02")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> classesService.update(1L, mockClassDto));
    }
    
    @Test
    void update_InvalidDates_ThrowsException() {
        when(classesRepository.findById(1L)).thenReturn(Optional.of(mockClass));
        mockClassDto.setStartDate(LocalDate.of(2024, 7, 1));

        assertThrows(OperationNotPermittedException.class, () -> classesService.update(1L, mockClassDto));
    }

    @Test
    void update_ClassNotFound_ThrowsException() {
        when(classesRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> classesService.update(1L, mockClassDto));
    }

    @Test
    void delete_Success() {
        when(classesRepository.findById(1L)).thenReturn(Optional.of(mockClass));
        classesService.delete(1L);
        verify(classesRepository).delete(mockClass);
    }

    @Test
    void getById_Success() {
        when(classesRepository.findById(1L)).thenReturn(Optional.of(mockClass));
        ClassesDto result = classesService.getById(1L);
        assertEquals("C01", result.getCode());
    }

    @Test
    void getAllClasses_Success() {
        Page<Classes> page = new PageImpl<>(List.of(mockClass));
        Pageable pageable = PageRequest.of(0, 10);
        when(classesRepository.searchClasses("C", 10L, 20L, pageable)).thenReturn(page);

        Page<ClassesDto> result = classesService.getAllClasses("C", 10L, 20L, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getMyClasses_Unauthenticated_ReturnsEmpty() {
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        List<ClassesDto> result = classesService.getMyClasses();

        assertTrue(result.isEmpty());
    }

    @Test
    void getMyClasses_Student_Success() {
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@student.com");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        User user = new User(); user.setId(100L);
        when(userRepository.findByEmail("test@student.com")).thenReturn(Optional.of(user));
        
        Student student = new Student(); student.setId(200L);
        when(studentRepository.findByUserId(100L)).thenReturn(Optional.of(student));
        when(staffRepository.findByUserId(100L)).thenReturn(Optional.empty());

        com.lms.education.module.enrollment.entity.Enrollment enrollment = new com.lms.education.module.enrollment.entity.Enrollment();
        enrollment.setClasses(mockClass);
        enrollment.setStatus("ACTIVE");
        
        when(enrollmentRepository.findByStudentId(200L)).thenReturn(List.of(enrollment));
        when(classesRepository.findAllById(anySet())).thenReturn(List.of(mockClass));

        List<ClassesDto> result = classesService.getMyClasses();

        assertEquals(1, result.size());
        assertEquals("C01", result.get(0).getCode());
    }
    
    @Test
    void recalculateEndDate_Success() {
        when(classesRepository.findById(1L)).thenReturn(Optional.of(mockClass));
        when(classScheduleRepository.findByClassesId(1L)).thenReturn(Collections.emptyList());
        when(classesRepository.save(any(Classes.class))).thenReturn(mockClass);
        
        classesService.recalculateEndDate(1L);
        
        verify(classesRepository).save(mockClass);
    }
    
    @Test
    void recalculateAllActiveClasses_Success() {
        when(classesRepository.findAll()).thenReturn(List.of(mockClass));
        when(classScheduleRepository.findByClassesId(1L)).thenReturn(Collections.emptyList());
        when(classesRepository.save(any(Classes.class))).thenReturn(mockClass);
        
        classesService.recalculateAllActiveClasses();
        
        verify(classesRepository).save(mockClass);
    }

    @Test
    void create_WithTermNotFound_ThrowsException() {
        when(classesRepository.existsByCode("C01")).thenReturn(false);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(mockCourse));
        when(termRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> classesService.create(mockClassDto));
    }

    @Test
    void create_WithNullOptionalFields() {
        mockClassDto.setMaxStudents(null);
        mockClassDto.setStatus(null);
        mockClassDto.setTermId(null);
        when(classesRepository.existsByCode("C01")).thenReturn(false);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(mockCourse));
        when(classesRepository.save(any(Classes.class))).thenAnswer(i -> i.getArgument(0));

        ClassesDto result = classesService.create(mockClassDto);
        assertEquals("OPENING", result.getStatus());
        assertEquals(20, result.getMaxStudents());
    }

    @Test
    void update_WithNullOptionalFields() {
        when(classesRepository.findById(1L)).thenReturn(Optional.of(mockClass));
        mockClassDto.setMaxStudents(null);
        mockClassDto.setStatus("  "); // empty string
        mockClassDto.setTermId(null);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(mockCourse));
        when(classesRepository.save(any(Classes.class))).thenAnswer(i -> i.getArgument(0));

        ClassesDto result = classesService.update(1L, mockClassDto);
        assertNotNull(result);
    }
    
    @Test
    void getMyClasses_Staff_Success() {
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("teacher@test.com");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        User user = new User(); user.setId(100L);
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(user));
        
        Staff staff = new Staff(); staff.setId(300L);
        when(staffRepository.findByUserId(100L)).thenReturn(Optional.of(staff));
        when(studentRepository.findByUserId(100L)).thenReturn(Optional.empty());

        when(scheduleAssignmentRepository.findClassIdsByTeacher(100L, 300L)).thenReturn(List.of(1L));
        
        com.lms.education.module.teaching.entity.TeachingAssignment ta = new com.lms.education.module.teaching.entity.TeachingAssignment();
        ta.setClasses(mockClass);
        when(teachingAssignmentRepository.findByTeacherId(300L)).thenReturn(List.of(ta));
        
        when(classesRepository.findAllById(anySet())).thenReturn(List.of(mockClass));

        List<ClassesDto> result = classesService.getMyClasses();
        assertEquals(1, result.size());
    }

    @Test
    void recalculateEndDate_WithNullCourse() {
        mockClass.setCourse(null);
        when(classesRepository.findById(1L)).thenReturn(Optional.of(mockClass));
        classesService.recalculateEndDate(1L);
        verify(classesRepository).save(any());
    }
    
    @Test
    void calculateExactEndDate_WithCancellation() {
        when(classesRepository.findById(1L)).thenReturn(Optional.of(mockClass));
        
        com.lms.education.module.academic.entity.ClassSchedule schedule = new com.lms.education.module.academic.entity.ClassSchedule();
        schedule.setDayOfWeek(2); // Monday
        when(classScheduleRepository.findByClassesId(1L)).thenReturn(List.of(schedule));
        
        com.lms.education.module.academic.entity.ScheduleCancellation cancel = new com.lms.education.module.academic.entity.ScheduleCancellation();
        cancel.setStartDate(LocalDate.of(2024, 1, 1)); // This is a Monday
        cancel.setEndDate(LocalDate.of(2024, 1, 1));
        when(scheduleCancellationRepository.findByClassIdOrCenterWide(1L)).thenReturn(List.of(cancel));
        
        when(classesRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        classesService.recalculateEndDate(1L);
        verify(classesRepository).save(any());
    }
}
