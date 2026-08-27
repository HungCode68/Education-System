package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.enrollment.entity.Enrollment;
import com.lms.education.module.enrollment.repository.EnrollmentRepository;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.user.dto.StudentDto;
import com.lms.education.module.user.dto.StudentProvisionDto;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.RoleRepository;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private StudentServiceImpl studentService;

    private Student mockStudent;
    private User mockUser;
    private StudentDto mockStudentDto;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@student.edu.vn");

        mockStudent = new Student();
        mockStudent.setId(1L);
        mockStudent.setStudentCode("HV260001");
        mockStudent.setFullName("Nguyen Van A");
        mockStudent.setStatus("STUDYING");

        mockStudentDto = StudentDto.builder()
                .fullName("Nguyen Van A")
                .status("STUDYING")
                .build();
    }

    @Test
    void create_Success_WithoutUser() {
        when(studentRepository.countByStudentCodePattern(anyString(), anyString())).thenReturn(0L);
        when(studentRepository.save(any(Student.class))).thenReturn(mockStudent);

        StudentDto result = studentService.create(mockStudentDto);

        assertNotNull(result);
        assertEquals("HV260001", result.getStudentCode());
        verify(userRepository, never()).findById(anyLong());
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void create_Success_WithValidUser() {
        mockStudentDto.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(studentRepository.existsByUserId(1L)).thenReturn(false);
        when(studentRepository.countByStudentCodePattern(anyString(), anyString())).thenReturn(0L);
        
        mockStudent.setUser(mockUser);
        when(studentRepository.save(any(Student.class))).thenReturn(mockStudent);

        StudentDto result = studentService.create(mockStudentDto);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void create_UserNotFound_ThrowsException() {
        mockStudentDto.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.create(mockStudentDto));
    }

    @Test
    void create_UserAlreadyLinked_ThrowsException() {
        mockStudentDto.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(studentRepository.existsByUserId(1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> studentService.create(mockStudentDto));
    }

    @Test
    void update_Success_WithoutUserChange() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        mockStudentDto.setFullName("Updated Name");
        when(studentRepository.save(any(Student.class))).thenReturn(mockStudent);

        StudentDto result = studentService.update(1L, mockStudentDto);

        assertNotNull(result);
        verify(studentRepository).save(mockStudent);
    }

    @Test
    void update_Success_WithNewUser() {
        mockStudentDto.setUserId(2L);
        User newUser = new User();
        newUser.setId(2L);
        
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(studentRepository.existsByUserId(2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(newUser));
        when(studentRepository.save(any(Student.class))).thenReturn(mockStudent);

        StudentDto result = studentService.update(1L, mockStudentDto);

        assertNotNull(result);
        verify(studentRepository).save(mockStudent);
    }

    @Test
    void update_NewUserAlreadyLinked_ThrowsException() {
        mockStudentDto.setUserId(2L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(studentRepository.existsByUserId(2L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> studentService.update(1L, mockStudentDto));
    }
    
    @Test
    void update_NewUserNotFound_ThrowsException() {
        mockStudentDto.setUserId(2L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(studentRepository.existsByUserId(2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.update(1L, mockStudentDto));
    }

    @Test
    void delete_Success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        studentService.delete(1L);
        verify(studentRepository).delete(mockStudent);
    }

    @Test
    void getById_Success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        StudentDto result = studentService.getById(1L);
        assertEquals("HV260001", result.getStudentCode());
    }

    @Test
    void getByStudentCode_Success() {
        when(studentRepository.findByStudentCode("HV260001")).thenReturn(Optional.of(mockStudent));
        StudentDto result = studentService.getByStudentCode("hv260001");
        assertEquals(1L, result.getId());
    }

    @Test
    void getAllStudents_WithKeyword() {
        Page<Student> page = new PageImpl<>(List.of(mockStudent));
        Pageable pageable = PageRequest.of(0, 10);
        when(studentRepository.searchStudents("Nguyen", pageable)).thenReturn(page);

        Page<StudentDto> result = studentService.getAllStudents("Nguyen", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllStudents_WithoutKeyword() {
        Page<Student> page = new PageImpl<>(List.of(mockStudent));
        Pageable pageable = PageRequest.of(0, 10);
        when(studentRepository.findAll(pageable)).thenReturn(page);

        Page<StudentDto> result = studentService.getAllStudents("  ", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void provisionAccounts_Success() {
        StudentProvisionDto dto = new StudentProvisionDto();
        dto.setRoleIds(List.of(1L));
        dto.setStudentIds(List.of(1L));

        Role role = new Role();
        role.setName("ROLE_STUDENT");
        when(roleRepository.findAllById(dto.getRoleIds())).thenReturn(List.of(role));

        when(studentRepository.findAllById(dto.getStudentIds())).thenReturn(List.of(mockStudent));
        when(userRepository.existsByEmail("hv260001@student.edu.vn")).thenReturn(false);
        when(passwordEncoder.encode("HV260001")).thenReturn("encoded");
        
        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setEmail("hv260001@student.edu.vn");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Map<String, Object> report = studentService.provisionAccounts(dto);

        assertEquals(1, report.get("success"));
        assertEquals(0, report.get("skipped"));
        assertEquals(1, report.get("totalProcessed"));
        assertEquals(savedUser, mockStudent.getUser());
    }

    @Test
    void provisionAccounts_NoRolesFound_ThrowsException() {
        StudentProvisionDto dto = new StudentProvisionDto();
        dto.setRoleIds(List.of(1L));
        when(roleRepository.findAllById(dto.getRoleIds())).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> studentService.provisionAccounts(dto));
    }

    @Test
    void provisionAccounts_SkipExistingEmail() {
        StudentProvisionDto dto = new StudentProvisionDto();
        dto.setRoleIds(List.of(1L));
        dto.setStudentIds(List.of(1L));

        when(roleRepository.findAllById(dto.getRoleIds())).thenReturn(List.of(new Role()));
        when(studentRepository.findAllById(dto.getStudentIds())).thenReturn(List.of(mockStudent));
        when(userRepository.existsByEmail("hv260001@student.edu.vn")).thenReturn(true); // email exists

        Map<String, Object> report = studentService.provisionAccounts(dto);

        assertEquals(0, report.get("success"));
        assertEquals(1, report.get("skipped"));
    }
    
    @Test
    void provisionAccounts_SkipAlreadyHasUser() {
        StudentProvisionDto dto = new StudentProvisionDto();
        dto.setRoleIds(List.of(1L));
        dto.setStudentIds(List.of(1L));

        mockStudent.setUser(mockUser); // already has user

        when(roleRepository.findAllById(dto.getRoleIds())).thenReturn(List.of(new Role()));
        when(studentRepository.findAllById(dto.getStudentIds())).thenReturn(List.of(mockStudent));

        Map<String, Object> report = studentService.provisionAccounts(dto);

        assertEquals(0, report.get("success"));
        assertEquals(1, report.get("skipped"));
    }

    @Test
    void getMyProfile_Success_WithEnrollment() {
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.of(mockStudent));
        Enrollment enrollment = new Enrollment();
        Classes classes = new Classes();
        classes.setId(5L);
        enrollment.setClasses(classes);
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(enrollment));

        StudentDto result = studentService.getMyProfile(1L);

        assertEquals("HV260001", result.getStudentCode());
        assertEquals(5L, result.getCurrentClassId());
    }

    @Test
    void getMyProfile_StudentNotFound_ThrowsException() {
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> studentService.getMyProfile(1L));
    }
}