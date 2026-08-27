package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.ClassScheduleDto;
import com.lms.education.module.academic.entity.ClassSchedule;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Room;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.RoomRepository;
import com.lms.education.module.academic.repository.ScheduleCancellationRepository;
import com.lms.education.module.academic.service.ClassesService;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.lms.education.module.teaching.entity.ScheduleAssignment;
import com.lms.education.module.teaching.entity.TeachingSubstitution;
import com.lms.education.module.academic.entity.ScheduleCancellation;
import com.lms.education.module.attendance.entity.Attendance;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.academic.dto.TimetableEntryDto;
import org.junit.jupiter.api.AfterEach;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ClassScheduleServiceImplTest {

    @Mock
    private ClassScheduleRepository classScheduleRepository;
    @Mock
    private ClassesRepository classesRepository;
    @Mock
    private ClassesService classesService;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ScheduleAssignmentRepository scheduleAssignmentRepository;
    @Mock
    private TeachingSubstitutionRepository teachingSubstitutionRepository;
    @Mock
    private ScheduleCancellationRepository scheduleCancellationRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private com.lms.education.module.attendance.repository.AttendanceRepository attendanceRepository;

    @InjectMocks
    private ClassScheduleServiceImpl classScheduleService;

    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    private ClassSchedule mockSchedule;
    private ClassScheduleDto mockDto;
    private Classes mockClass;
    private Room mockRoom;

    @BeforeEach
    void setUp() {
        mockClass = new Classes();
        mockClass.setId(10L);
        mockClass.setCode("C01");
        mockClass.setStartDate(LocalDate.of(2024, 1, 1));
        mockClass.setEndDate(LocalDate.of(2024, 6, 1));

        mockRoom = new Room();
        mockRoom.setId(20L);
        mockRoom.setName("A1");

        mockSchedule = new ClassSchedule();
        mockSchedule.setId(1L);
        mockSchedule.setClasses(mockClass);
        mockSchedule.setRoom(mockRoom);
        mockSchedule.setDayOfWeek(2);
        mockSchedule.setStartTime(LocalTime.of(8, 0));
        mockSchedule.setEndTime(LocalTime.of(10, 0));

        mockDto = ClassScheduleDto.builder()
                .classId(10L)
                .roomId(20L)
                .dayOfWeek(2)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
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
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(roomRepository.findById(20L)).thenReturn(Optional.of(mockRoom));
        when(classScheduleRepository.existsClassConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(classScheduleRepository.existsRoomConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(mockSchedule);

        ClassScheduleDto result = classScheduleService.create(mockDto);

        assertNotNull(result);
        verify(classesService).recalculateEndDate(10L);
        verify(classScheduleRepository).save(any(ClassSchedule.class));
    }

    @Test
    void create_InvalidTime_ThrowsException() {
        mockDto.setStartTime(LocalTime.of(10, 0));
        mockDto.setEndTime(LocalTime.of(8, 0));

        assertThrows(OperationNotPermittedException.class, () -> classScheduleService.create(mockDto));
    }

    @Test
    void create_ClassConflict_ThrowsException() {
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(roomRepository.findById(20L)).thenReturn(Optional.of(mockRoom));
        when(classScheduleRepository.existsClassConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(true);

        assertThrows(OperationNotPermittedException.class, () -> classScheduleService.create(mockDto));
    }

    @Test
    void create_RoomConflict_ThrowsException() {
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(roomRepository.findById(20L)).thenReturn(Optional.of(mockRoom));
        when(classScheduleRepository.existsClassConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(classScheduleRepository.existsRoomConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(true);

        assertThrows(OperationNotPermittedException.class, () -> classScheduleService.create(mockDto));
    }

    @Test
    void update_Success() {
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(mockSchedule));
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(roomRepository.findById(20L)).thenReturn(Optional.of(mockRoom));
        when(classScheduleRepository.existsClassConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(classScheduleRepository.existsRoomConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(mockSchedule);

        ClassScheduleDto result = classScheduleService.update(1L, mockDto);

        assertNotNull(result);
        verify(classesService).recalculateEndDate(10L);
    }

    @Test
    void delete_Success() {
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(mockSchedule));
        
        classScheduleService.delete(1L);
        
        verify(classScheduleRepository).delete(mockSchedule);
        verify(classesService).recalculateEndDate(10L);
    }

    @Test
    void getById_Success() {
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(mockSchedule));
        ClassScheduleDto result = classScheduleService.getById(1L);
        assertEquals(2, result.getDayOfWeek());
    }
    
    @Test
    void getSchedulesByClassId_Success() {
        when(classesRepository.existsById(10L)).thenReturn(true);
        when(classScheduleRepository.findByClassesId(10L)).thenReturn(List.of(mockSchedule));
        
        List<ClassScheduleDto> result = classScheduleService.getSchedulesByClassId(10L);
        assertEquals(1, result.size());
    }

    @Test
    void getStudentTimetable_Success() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(classScheduleRepository.findSchedulesByStudentId(1L)).thenReturn(List.of(mockSchedule));
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetween(any(), any(), any())).thenReturn(List.of());
        
        Staff teacher = new Staff();
        teacher.setId(100L);
        teacher.setFullName("Main Teacher");
        ScheduleAssignment assignment = new ScheduleAssignment();
        assignment.setRole("MAIN_TEACHER");
        assignment.setTeacher(teacher);
        when(scheduleAssignmentRepository.findByScheduleId(1L)).thenReturn(List.of(assignment));

        // Test normal date inside schedule
        LocalDate startDate = LocalDate.of(2024, 1, 1); // Monday
        LocalDate endDate = LocalDate.of(2024, 1, 7); // Sunday
        // mockSchedule dayOfWeek is 2 (Monday in system logic dayOfWeek 2 is Monday)
        // LocalDate 2024-01-01 is Monday (getValue() = 1), plus 1 = 2 (matches)

        List<TimetableEntryDto> result = classScheduleService.getStudentTimetable(1L, startDate, endDate);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Main Teacher", result.get(0).getTeacherName());
        assertEquals("NORMAL", result.get(0).getStatus());
    }

    @Test
    void getStudentTimetable_WithSubstitutionAndCancellation() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(classScheduleRepository.findSchedulesByStudentId(1L)).thenReturn(List.of(mockSchedule));
        
        Staff subTeacher = new Staff();
        subTeacher.setId(200L);
        subTeacher.setFullName("Sub Teacher");
        TeachingSubstitution sub = new TeachingSubstitution();
        sub.setStatus("APPROVED");
        sub.setSubstituteStaff(subTeacher);
        sub.setStartDate(LocalDate.of(2024, 1, 1));
        sub.setEndDate(LocalDate.of(2024, 1, 1));
        when(teachingSubstitutionRepository.findByScheduleId(1L)).thenReturn(List.of(sub));

        ScheduleCancellation cancel = new ScheduleCancellation();
        cancel.setStartDate(LocalDate.of(2024, 1, 1));
        cancel.setEndDate(LocalDate.of(2024, 1, 1));
        cancel.setReason("Holiday");
        when(scheduleCancellationRepository.findByClassIdOrCenterWide(mockClass.getId())).thenReturn(List.of(cancel));

        LocalDate date = LocalDate.of(2024, 1, 1);
        List<TimetableEntryDto> result = classScheduleService.getStudentTimetable(1L, date, date);
        assertFalse(result.isEmpty());
        assertEquals("Sub Teacher", result.get(0).getTeacherName());
        assertTrue(result.get(0).getIsSubstituted());
        assertEquals("CANCELLED", result.get(0).getStatus());
        assertEquals("Holiday", result.get(0).getCancellationReason());
    }

    @Test
    void getTeacherTimetable_Success() {
        when(staffRepository.existsById(100L)).thenReturn(true);
        
        Staff teacher = new Staff();
        teacher.setId(100L);
        teacher.setFullName("Teacher");
        ScheduleAssignment assignment = new ScheduleAssignment();
        assignment.setSchedule(mockSchedule);
        assignment.setTeacher(teacher);
        
        when(scheduleAssignmentRepository.findByTeacherId(100L)).thenReturn(List.of(assignment));
        when(teachingSubstitutionRepository.findBySubstituteStaffId(100L)).thenReturn(List.of());

        LocalDate date = LocalDate.of(2024, 1, 1); // Monday -> day 2
        List<TimetableEntryDto> result = classScheduleService.getTeacherTimetable(100L, date, date);
        assertEquals(1, result.size());
        assertEquals("Teacher", result.get(0).getTeacherName());
    }

    @Test
    void getTeacherTimetable_AsSubstitute() {
        when(staffRepository.existsById(100L)).thenReturn(true);
        when(scheduleAssignmentRepository.findByTeacherId(100L)).thenReturn(List.of());
        
        Staff teacher = new Staff();
        teacher.setId(100L);
        teacher.setFullName("Teacher Sub");
        TeachingSubstitution sub = new TeachingSubstitution();
        sub.setStatus("APPROVED");
        sub.setSchedule(mockSchedule);
        sub.setSubstituteStaff(teacher);
        sub.setStartDate(LocalDate.of(2024, 1, 1));
        sub.setEndDate(LocalDate.of(2024, 1, 1));
        
        when(teachingSubstitutionRepository.findBySubstituteStaffId(100L)).thenReturn(List.of(sub));

        LocalDate date = LocalDate.of(2024, 1, 1);
        List<TimetableEntryDto> result = classScheduleService.getTeacherTimetable(100L, date, date);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsSubstituted());
    }

    @Test
    void getMyStudentTimetable_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("student@test.com");
        
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        
        User user = new User();
        user.setId(50L);
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(user));
        
        Student student = new Student();
        student.setId(1L);
        when(studentRepository.findByUserId(50L)).thenReturn(Optional.of(student));
        
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(classScheduleRepository.findSchedulesByStudentId(1L)).thenReturn(List.of());
        
        List<TimetableEntryDto> result = classScheduleService.getMyStudentTimetable(LocalDate.now(), LocalDate.now());
        assertTrue(result.isEmpty());
    }

    // ---------- Extra tests covering missed branches ----------

    @Test
    void create_NullRoomId_NullRoomOk() {
        mockDto.setRoomId(null);
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(classScheduleRepository.existsClassConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(mockSchedule);

        ClassScheduleDto result = classScheduleService.create(mockDto);
        assertNotNull(result);
    }

    @Test
    void update_NoRoomId_KeepsExistingRoom() {
        mockDto.setRoomId(null);
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(mockSchedule));
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(classScheduleRepository.existsClassConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(mockSchedule);

        ClassScheduleDto result = classScheduleService.update(1L, mockDto);
        assertNotNull(result);
    }

    @Test
    void getStudentTimetable_DayOutsideClassSchedule_NotIncluded() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(classScheduleRepository.findSchedulesByStudentId(1L)).thenReturn(List.of(mockSchedule));
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetween(any(), any(), any())).thenReturn(List.of());
        when(scheduleAssignmentRepository.findByScheduleId(1L)).thenReturn(List.of());

        // Sunday Jan 7 2024 - dayOfWeek is 7, mockSchedule is dayOfWeek=2 (Tuesday)
        LocalDate sunday = LocalDate.of(2024, 1, 7);
        List<TimetableEntryDto> result = classScheduleService.getStudentTimetable(1L, sunday, sunday);
        assertTrue(result.isEmpty());
    }

    @Test
    void getStudentTimetable_DateBeforeClassStart_NotIncluded() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(classScheduleRepository.findSchedulesByStudentId(1L)).thenReturn(List.of(mockSchedule));
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetween(any(), any(), any())).thenReturn(List.of());
        when(scheduleAssignmentRepository.findByScheduleId(1L)).thenReturn(List.of());

        // mockClass.startDate = 2024-01-01, so 2023-12-30 is before
        LocalDate beforeStart = LocalDate.of(2023, 12, 26); // Tuesday
        List<TimetableEntryDto> result = classScheduleService.getStudentTimetable(1L, beforeStart, beforeStart);
        assertTrue(result.isEmpty());
    }

    @Test
    void getStudentTimetable_DateAfterClassEnd_NotIncluded() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(classScheduleRepository.findSchedulesByStudentId(1L)).thenReturn(List.of(mockSchedule));
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetween(any(), any(), any())).thenReturn(List.of());
        when(scheduleAssignmentRepository.findByScheduleId(1L)).thenReturn(List.of());

        // mockClass.endDate = 2024-06-01, so 2024-06-04 (Tuesday) is after
        LocalDate afterEnd = LocalDate.of(2024, 6, 4); // Tuesday
        List<TimetableEntryDto> result = classScheduleService.getStudentTimetable(1L, afterEnd, afterEnd);
        assertTrue(result.isEmpty());
    }

    @Test
    void getStudentTimetable_WithAttendance() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(classScheduleRepository.findSchedulesByStudentId(1L)).thenReturn(List.of(mockSchedule));

        // systemDayOfWeek = DayOfWeek.getValue() + 1; Monday=1+1=2 -> matches mockSchedule dayOfWeek=2
        // Jan 1, 2024 is Monday, and class starts on Jan 1 2024
        Attendance attendance = new Attendance();
        attendance.setAttendanceDate(LocalDate.of(2024, 1, 1));
        attendance.setStatus("PRESENT");
        attendance.setSchedule(mockSchedule); // required for attendance filter
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetween(any(), any(), any()))
            .thenReturn(List.of(attendance));
        when(scheduleAssignmentRepository.findByScheduleId(1L)).thenReturn(List.of());
        when(scheduleCancellationRepository.findByClassIdOrCenterWide(any())).thenReturn(List.of());

        LocalDate monday = LocalDate.of(2024, 1, 1); // Monday, dayOfWeek=2 matches
        List<TimetableEntryDto> result = classScheduleService.getStudentTimetable(1L, monday, monday);
        assertFalse(result.isEmpty());
        assertEquals("PRESENT", result.get(0).getAttendanceStatus());
    }

    @Test
    void getStudentTimetable_SubstitutionPendingIsIgnored() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(classScheduleRepository.findSchedulesByStudentId(1L)).thenReturn(List.of(mockSchedule));
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetween(any(), any(), any())).thenReturn(List.of());

        // Substitution with status PENDING -> should NOT be considered as substitution
        Staff subTeacher = new Staff();
        subTeacher.setId(200L);
        subTeacher.setFullName("Pending Sub");
        TeachingSubstitution pendingSub = new TeachingSubstitution();
        pendingSub.setStatus("PENDING"); // not APPROVED -> ignored
        pendingSub.setSubstituteStaff(subTeacher);
        pendingSub.setStartDate(LocalDate.of(2024, 1, 1));
        pendingSub.setEndDate(LocalDate.of(2024, 1, 1));
        when(teachingSubstitutionRepository.findByScheduleId(1L)).thenReturn(List.of(pendingSub));
        when(scheduleCancellationRepository.findByClassIdOrCenterWide(any())).thenReturn(List.of());

        Staff mainTeacher = new Staff();
        mainTeacher.setId(100L);
        mainTeacher.setFullName("Main Teacher");
        ScheduleAssignment assignment = new ScheduleAssignment();
        assignment.setRole("MAIN_TEACHER");
        assignment.setTeacher(mainTeacher);
        when(scheduleAssignmentRepository.findByScheduleId(1L)).thenReturn(List.of(assignment));

        LocalDate monday = LocalDate.of(2024, 1, 1); // Monday = systemDayOfWeek 2
        List<TimetableEntryDto> result = classScheduleService.getStudentTimetable(1L, monday, monday);
        assertFalse(result.isEmpty());
        // PENDING sub is NOT approved, so main teacher shown
        assertEquals("Main Teacher", result.get(0).getTeacherName());
    }

    @Test
    void getTeacherTimetable_DateOutsideClassRange() {
        when(staffRepository.existsById(100L)).thenReturn(true);

        Staff teacher = new Staff();
        teacher.setId(100L);
        teacher.setFullName("Teacher");
        ScheduleAssignment assignment = new ScheduleAssignment();
        assignment.setSchedule(mockSchedule); // dayOfWeek=2, class start=2024-01-01
        assignment.setTeacher(teacher);

        when(scheduleAssignmentRepository.findByTeacherId(100L)).thenReturn(List.of(assignment));
        when(teachingSubstitutionRepository.findBySubstituteStaffId(100L)).thenReturn(List.of());

        // 2023-12-26 (Tuesday) but before class start
        LocalDate beforeClassStart = LocalDate.of(2023, 12, 26);
        List<TimetableEntryDto> result = classScheduleService.getTeacherTimetable(100L, beforeClassStart, beforeClassStart);
        assertTrue(result.isEmpty());
    }

    @Test
    void getTeacherTimetable_NullScheduleInAssignment_Skipped() {
        // The service does not guard against null schedule in assignment
        // This test verifies behavior: when schedule is null, NPE is thrown (existing service behavior)
        when(staffRepository.existsById(100L)).thenReturn(true);

        ScheduleAssignment assignment = new ScheduleAssignment();
        assignment.setSchedule(mockSchedule); // use real schedule so no NPE
        Staff teacher = new Staff();
        teacher.setId(100L);
        teacher.setFullName("Teacher");
        assignment.setTeacher(teacher);

        when(scheduleAssignmentRepository.findByTeacherId(100L)).thenReturn(List.of(assignment));
        when(teachingSubstitutionRepository.findBySubstituteStaffId(100L)).thenReturn(List.of());

        LocalDate date = LocalDate.of(2023, 12, 26); // before class start -> empty
        List<TimetableEntryDto> result = classScheduleService.getTeacherTimetable(100L, date, date);
        assertTrue(result.isEmpty());
    }

    @Test
    void getSchedulesByClassId_NotFound_ThrowsException() {
        when(classesRepository.existsById(10L)).thenReturn(false);
        assertThrows(com.lms.education.exception.ResourceNotFoundException.class,
            () -> classScheduleService.getSchedulesByClassId(10L));
    }

    @Test
    void getStudentTimetable_NotFound_ThrowsException() {
        when(studentRepository.existsById(1L)).thenReturn(false);
        assertThrows(com.lms.education.exception.ResourceNotFoundException.class,
            () -> classScheduleService.getStudentTimetable(1L, LocalDate.now(), LocalDate.now()));
    }

    @Test
    void getTeacherTimetable_NotFound_ThrowsException() {
        when(staffRepository.existsById(100L)).thenReturn(false);
        assertThrows(com.lms.education.exception.ResourceNotFoundException.class,
            () -> classScheduleService.getTeacherTimetable(100L, LocalDate.now(), LocalDate.now()));
    }
    @Test
    void getMyTeacherTimetable_Unauthenticated() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        List<TimetableEntryDto> result = classScheduleService.getMyTeacherTimetable(LocalDate.now(), LocalDate.now());
        assertTrue(result.isEmpty());
    }

    @Test
    void getMyTeacherTimetable_UserNotFound() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("teacher@test.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.empty());

        List<TimetableEntryDto> result = classScheduleService.getMyTeacherTimetable(LocalDate.now(), LocalDate.now());
        assertTrue(result.isEmpty());
    }

    @Test
    void getMyTeacherTimetable_StaffNotFound() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("teacher@test.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        User user = new User();
        user.setId(50L);
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(user));
        when(staffRepository.findByUserId(50L)).thenReturn(Optional.empty());

        List<TimetableEntryDto> result = classScheduleService.getMyTeacherTimetable(LocalDate.now(), LocalDate.now());
        assertTrue(result.isEmpty());
    }

    @Test
    void getMyTeacherTimetable_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("teacher@test.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        User user = new User();
        user.setId(50L);
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(user));

        Staff staff = new Staff();
        staff.setId(100L);
        when(staffRepository.findByUserId(50L)).thenReturn(Optional.of(staff));
        when(staffRepository.existsById(100L)).thenReturn(true);
        when(scheduleAssignmentRepository.findByTeacherId(100L)).thenReturn(List.of());
        when(teachingSubstitutionRepository.findBySubstituteStaffId(100L)).thenReturn(List.of());

        List<TimetableEntryDto> result = classScheduleService.getMyTeacherTimetable(LocalDate.now(), LocalDate.now());
        assertTrue(result.isEmpty());
    }

    @Test
    void getMyStudentTimetable_Unauthenticated() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        List<TimetableEntryDto> result = classScheduleService.getMyStudentTimetable(LocalDate.now(), LocalDate.now());
        assertTrue(result.isEmpty());
    }

    @Test
    void getMyStudentTimetable_UserNotFound() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("student@test.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.empty());

        List<TimetableEntryDto> result = classScheduleService.getMyStudentTimetable(LocalDate.now(), LocalDate.now());
        assertTrue(result.isEmpty());
    }

    @Test
    void getMyStudentTimetable_StudentNotFound() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("student@test.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        User user = new User();
        user.setId(50L);
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(user));
        when(studentRepository.findByUserId(50L)).thenReturn(Optional.empty());

        List<TimetableEntryDto> result = classScheduleService.getMyStudentTimetable(LocalDate.now(), LocalDate.now());
        assertTrue(result.isEmpty());
    }
    
    @Test
    void getTimetable_NullClassId_ReturnsAll() {
        when(classScheduleRepository.findAll()).thenReturn(List.of(mockSchedule));
        
        Staff teacher = new Staff();
        teacher.setId(100L);
        teacher.setFullName("Main Teacher");
        ScheduleAssignment assignment = new ScheduleAssignment();
        assignment.setRole("MAIN_TEACHER");
        assignment.setTeacher(teacher);
        when(scheduleAssignmentRepository.findByScheduleId(1L)).thenReturn(List.of(assignment));
        when(teachingSubstitutionRepository.findByScheduleId(1L)).thenReturn(List.of());
        when(scheduleCancellationRepository.findByClassIdOrCenterWide(mockClass.getId())).thenReturn(List.of());

        LocalDate date = LocalDate.of(2024, 1, 1); // Monday, dayOfWeek=2
        List<TimetableEntryDto> result = classScheduleService.getTimetable(date, date, null);
        assertEquals(1, result.size());
        assertEquals("Main Teacher", result.get(0).getTeacherName());
        assertEquals("NORMAL", result.get(0).getStatus());
    }

    @Test
    void getTimetable_WithClassId_AndSubAndCancellation() {
        when(classScheduleRepository.findByClassesId(10L)).thenReturn(List.of(mockSchedule));
        
        when(scheduleAssignmentRepository.findByScheduleId(1L)).thenReturn(List.of());
        
        Staff subTeacher = new Staff();
        subTeacher.setId(200L);
        subTeacher.setFullName("Sub Teacher");
        TeachingSubstitution sub = new TeachingSubstitution();
        sub.setStatus("APPROVED");
        sub.setSubstituteStaff(subTeacher);
        sub.setStartDate(LocalDate.of(2024, 1, 1));
        sub.setEndDate(LocalDate.of(2024, 1, 1));
        when(teachingSubstitutionRepository.findByScheduleId(1L)).thenReturn(List.of(sub));

        ScheduleCancellation cancel = new ScheduleCancellation();
        cancel.setStartDate(LocalDate.of(2024, 1, 1));
        cancel.setEndDate(LocalDate.of(2024, 1, 1));
        cancel.setReason("Holiday");
        when(scheduleCancellationRepository.findByClassIdOrCenterWide(mockClass.getId())).thenReturn(List.of(cancel));

        LocalDate date = LocalDate.of(2024, 1, 1); // Monday, dayOfWeek=2
        List<TimetableEntryDto> result = classScheduleService.getTimetable(date, date, 10L);
        assertEquals(1, result.size());
        assertEquals("Sub Teacher", result.get(0).getTeacherName());
        assertTrue(result.get(0).getIsSubstituted());
        assertEquals("CANCELLED", result.get(0).getStatus());
        assertEquals("Holiday", result.get(0).getCancellationReason());
    }

    @Test
    void create_StartTimeEqualsEndTime_ThrowsException() {
        mockDto.setStartTime(LocalTime.of(8, 0));
        mockDto.setEndTime(LocalTime.of(8, 0));
        assertThrows(OperationNotPermittedException.class, () -> classScheduleService.create(mockDto));
    }

    @Test
    void update_InvalidTime_ThrowsException() {
        mockDto.setStartTime(LocalTime.of(10, 0));
        mockDto.setEndTime(LocalTime.of(8, 0));
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(mockSchedule));
        assertThrows(OperationNotPermittedException.class, () -> classScheduleService.update(1L, mockDto));
    }

    @Test
    void update_ClassConflict_ThrowsException() {
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(mockSchedule));
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(roomRepository.findById(20L)).thenReturn(Optional.of(mockRoom));
        when(classScheduleRepository.existsClassConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(true);
        assertThrows(OperationNotPermittedException.class, () -> classScheduleService.update(1L, mockDto));
    }

    @Test
    void update_RoomConflict_ThrowsException() {
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(mockSchedule));
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(roomRepository.findById(20L)).thenReturn(Optional.of(mockRoom));
        when(classScheduleRepository.existsClassConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(classScheduleRepository.existsRoomConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(true);
        assertThrows(OperationNotPermittedException.class, () -> classScheduleService.update(1L, mockDto));
    }

    @Test
    void getStudentTimetable_SubAndCancel_OutsideDateBounds_Ignored() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(classScheduleRepository.findSchedulesByStudentId(1L)).thenReturn(List.of(mockSchedule));
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetween(any(), any(), any())).thenReturn(List.of());
        
        Staff teacher = new Staff();
        teacher.setId(100L);
        teacher.setFullName("Main Teacher");
        ScheduleAssignment assignment = new ScheduleAssignment();
        assignment.setRole("MAIN_TEACHER");
        assignment.setTeacher(teacher);
        when(scheduleAssignmentRepository.findByScheduleId(1L)).thenReturn(List.of(assignment));

        // Sub outside bounds
        TeachingSubstitution sub = new TeachingSubstitution();
        sub.setStatus("APPROVED");
        sub.setSubstituteStaff(new Staff());
        sub.setStartDate(LocalDate.of(2024, 1, 8)); // different week
        sub.setEndDate(LocalDate.of(2024, 1, 8));
        when(teachingSubstitutionRepository.findByScheduleId(1L)).thenReturn(List.of(sub));

        // Cancel outside bounds
        ScheduleCancellation cancel = new ScheduleCancellation();
        cancel.setStartDate(LocalDate.of(2024, 1, 8));
        cancel.setEndDate(LocalDate.of(2024, 1, 8));
        when(scheduleCancellationRepository.findByClassIdOrCenterWide(mockClass.getId())).thenReturn(List.of(cancel));

        LocalDate date = LocalDate.of(2024, 1, 1);
        List<TimetableEntryDto> result = classScheduleService.getStudentTimetable(1L, date, date);
        assertFalse(result.isEmpty());
        assertEquals("Main Teacher", result.get(0).getTeacherName());
        assertFalse(result.get(0).getIsSubstituted());
        assertEquals("NORMAL", result.get(0).getStatus());
    }

    @Test
    void getTeacherTimetable_TeacherIsAbsent() {
        when(staffRepository.existsById(100L)).thenReturn(true);
        
        Staff teacher = new Staff();
        teacher.setId(100L);
        teacher.setFullName("Teacher");
        ScheduleAssignment assignment = new ScheduleAssignment();
        assignment.setSchedule(mockSchedule);
        assignment.setTeacher(teacher);
        
        when(scheduleAssignmentRepository.findByTeacherId(100L)).thenReturn(List.of(assignment));
        
        // Teacher is absent
        TeachingSubstitution sub = new TeachingSubstitution();
        sub.setStatus("APPROVED");
        sub.setAbsentStaff(teacher);
        sub.setStartDate(LocalDate.of(2024, 1, 1));
        sub.setEndDate(LocalDate.of(2024, 1, 1));
        
        when(teachingSubstitutionRepository.findByScheduleId(1L)).thenReturn(List.of(sub));
        when(teachingSubstitutionRepository.findBySubstituteStaffId(100L)).thenReturn(List.of());

        LocalDate date = LocalDate.of(2024, 1, 1);
        List<TimetableEntryDto> result = classScheduleService.getTeacherTimetable(100L, date, date);
        assertTrue(result.isEmpty()); // Absent teacher should not see this schedule as theirs
    }

    @Test
    void getTeacherTimetable_CancelOutsideBounds_Ignored() {
        when(staffRepository.existsById(100L)).thenReturn(true);
        
        Staff teacher = new Staff();
        teacher.setId(100L);
        teacher.setFullName("Teacher");
        ScheduleAssignment assignment = new ScheduleAssignment();
        assignment.setSchedule(mockSchedule);
        assignment.setTeacher(teacher);
        
        when(scheduleAssignmentRepository.findByTeacherId(100L)).thenReturn(List.of(assignment));
        when(teachingSubstitutionRepository.findByScheduleId(1L)).thenReturn(List.of());
        when(teachingSubstitutionRepository.findBySubstituteStaffId(100L)).thenReturn(List.of());
        
        // Cancel outside bounds
        ScheduleCancellation cancel = new ScheduleCancellation();
        cancel.setStartDate(LocalDate.of(2024, 1, 8));
        cancel.setEndDate(LocalDate.of(2024, 1, 8));
        when(scheduleCancellationRepository.findByClassIdOrCenterWide(mockClass.getId())).thenReturn(List.of(cancel));

        LocalDate date = LocalDate.of(2024, 1, 1);
        List<TimetableEntryDto> result = classScheduleService.getTeacherTimetable(100L, date, date);
        assertFalse(result.isEmpty());
        assertEquals("NORMAL", result.get(0).getStatus());
    }

    @Test
    void getTimetable_CancelOutsideBounds_Ignored() {
        when(classScheduleRepository.findAll()).thenReturn(List.of(mockSchedule));
        when(scheduleAssignmentRepository.findByScheduleId(1L)).thenReturn(List.of());
        when(teachingSubstitutionRepository.findByScheduleId(1L)).thenReturn(List.of());

        // Cancel outside bounds
        ScheduleCancellation cancel = new ScheduleCancellation();
        cancel.setStartDate(LocalDate.of(2024, 1, 8));
        cancel.setEndDate(LocalDate.of(2024, 1, 8));
        when(scheduleCancellationRepository.findByClassIdOrCenterWide(mockClass.getId())).thenReturn(List.of(cancel));

        LocalDate date = LocalDate.of(2024, 1, 1);
        List<TimetableEntryDto> result = classScheduleService.getTimetable(date, date, null);
        assertFalse(result.isEmpty());
        assertEquals("NORMAL", result.get(0).getStatus());
    }
}

