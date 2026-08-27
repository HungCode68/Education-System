package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.AccountProvisionDto;
import com.lms.education.module.user.dto.StaffDto;
import com.lms.education.module.user.entity.Department;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.DepartmentRepository;
import com.lms.education.module.user.repository.RoleRepository;
import com.lms.education.module.user.repository.StaffRepository;
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
public class StaffServiceImplTest {

    @Mock
    private StaffRepository staffRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StaffServiceImpl staffService;

    private Staff mockStaff;
    private User mockUser;
    private Department mockDepartment;
    private StaffDto mockStaffDto;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@school.edu.vn");

        mockDepartment = new Department();
        mockDepartment.setId(1L);
        mockDepartment.setCode("IT");

        mockStaff = new Staff();
        mockStaff.setId(1L);
        mockStaff.setStaffCode("IT260001");
        mockStaff.setFullName("Nguyen Van Staff");
        mockStaff.setStaffType("TEACHER");
        mockStaff.setStatus("ACTIVE");
        mockStaff.setDepartment(mockDepartment);

        mockStaffDto = StaffDto.builder()
                .fullName("Nguyen Van Staff")
                .staffType("TEACHER")
                .status("ACTIVE")
                .build();
    }

    @Test
    void create_Success_WithoutUserAndDepartment() {
        when(staffRepository.countByStaffCodePattern(anyString(), anyString())).thenReturn(0L);
        when(staffRepository.save(any(Staff.class))).thenReturn(mockStaff);

        StaffDto result = staffService.create(mockStaffDto);

        assertNotNull(result);
        assertEquals("IT260001", result.getStaffCode());
        verify(userRepository, never()).findById(anyLong());
        verify(departmentRepository, never()).findById(anyLong());
    }

    @Test
    void create_Success_WithUserAndDepartment() {
        mockStaffDto.setUserId(1L);
        mockStaffDto.setDepartmentId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(staffRepository.existsByUserId(1L)).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mockDepartment));
        when(staffRepository.countByStaffCodePattern(anyString(), anyString())).thenReturn(0L);
        
        mockStaff.setUser(mockUser);
        when(staffRepository.save(any(Staff.class))).thenReturn(mockStaff);

        StaffDto result = staffService.create(mockStaffDto);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(1L, result.getDepartmentId());
    }

    @Test
    void create_UserNotFound_ThrowsException() {
        mockStaffDto.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> staffService.create(mockStaffDto));
    }

    @Test
    void create_UserAlreadyLinked_ThrowsException() {
        mockStaffDto.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(staffRepository.existsByUserId(1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> staffService.create(mockStaffDto));
    }

    @Test
    void create_DepartmentNotFound_ThrowsException() {
        mockStaffDto.setDepartmentId(1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> staffService.create(mockStaffDto));
    }

    @Test
    void update_Success_WithoutUserAndDepartmentChange() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(mockStaff));
        mockStaffDto.setFullName("Updated Name");
        when(staffRepository.save(any(Staff.class))).thenReturn(mockStaff);

        StaffDto result = staffService.update(1L, mockStaffDto);

        assertNotNull(result);
        verify(staffRepository).save(mockStaff);
    }

    @Test
    void update_Success_WithNewUserAndDepartment() {
        mockStaffDto.setUserId(2L);
        mockStaffDto.setDepartmentId(2L);
        
        User newUser = new User();
        newUser.setId(2L);
        Department newDept = new Department();
        newDept.setId(2L);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(mockStaff));
        when(staffRepository.existsByUserId(2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(newUser));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
        when(staffRepository.save(any(Staff.class))).thenReturn(mockStaff);

        StaffDto result = staffService.update(1L, mockStaffDto);

        assertNotNull(result);
    }

    @Test
    void update_NewUserAlreadyLinked_ThrowsException() {
        mockStaffDto.setUserId(2L);
        when(staffRepository.findById(1L)).thenReturn(Optional.of(mockStaff));
        when(staffRepository.existsByUserId(2L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> staffService.update(1L, mockStaffDto));
    }

    @Test
    void update_DepartmentNotFound_ThrowsException() {
        mockStaffDto.setDepartmentId(2L);
        when(staffRepository.findById(1L)).thenReturn(Optional.of(mockStaff));
        when(departmentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> staffService.update(1L, mockStaffDto));
    }

    @Test
    void delete_Success() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(mockStaff));
        staffService.delete(1L);
        verify(staffRepository).delete(mockStaff);
    }

    @Test
    void getById_Success() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(mockStaff));
        StaffDto result = staffService.getById(1L);
        assertEquals("IT260001", result.getStaffCode());
    }

    @Test
    void getByStaffCode_Success() {
        when(staffRepository.findByStaffCode("IT260001")).thenReturn(Optional.of(mockStaff));
        StaffDto result = staffService.getByStaffCode("it260001");
        assertEquals(1L, result.getId());
    }

    @Test
    void getAllStaffs_WithKeyword() {
        Page<Staff> page = new PageImpl<>(List.of(mockStaff));
        Pageable pageable = PageRequest.of(0, 10);
        when(staffRepository.searchStaffs("Nguyen", pageable)).thenReturn(page);

        Page<StaffDto> result = staffService.getAllStaffs("Nguyen", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllStaffs_WithoutKeyword() {
        Page<Staff> page = new PageImpl<>(List.of(mockStaff));
        Pageable pageable = PageRequest.of(0, 10);
        when(staffRepository.findAll(pageable)).thenReturn(page);

        Page<StaffDto> result = staffService.getAllStaffs("  ", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStaffsByDepartmentId_Success() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        Page<Staff> page = new PageImpl<>(List.of(mockStaff));
        Pageable pageable = PageRequest.of(0, 10);
        when(staffRepository.findByDepartmentId(1L, pageable)).thenReturn(page);

        Page<StaffDto> result = staffService.getStaffsByDepartmentId(1L, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStaffsByDepartmentId_DeptNotFound_ThrowsException() {
        when(departmentRepository.existsById(1L)).thenReturn(false);
        Pageable pageable = PageRequest.of(0, 10);
        assertThrows(ResourceNotFoundException.class, () -> staffService.getStaffsByDepartmentId(1L, pageable));
    }

    @Test
    void getTeachers_Success() {
        when(staffRepository.findTeachers()).thenReturn(List.of(mockStaff));
        List<StaffDto> result = staffService.getTeachers();
        assertEquals(1, result.size());
    }

    @Test
    void provisionAccounts_Success() {
        AccountProvisionDto dto = new AccountProvisionDto();
        dto.setRoleIds(List.of(1L));
        dto.setStaffIds(List.of(1L));

        Role role = new Role();
        role.setName("ROLE_TEACHER");
        when(roleRepository.findAllById(dto.getRoleIds())).thenReturn(List.of(role));

        when(staffRepository.findAllById(dto.getStaffIds())).thenReturn(List.of(mockStaff));
        when(userRepository.existsByEmail("it260001@school.edu.vn")).thenReturn(false);
        when(passwordEncoder.encode("IT260001")).thenReturn("encoded");
        
        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setEmail("it260001@school.edu.vn");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Map<String, Object> report = staffService.provisionAccounts(dto);

        assertEquals(1, report.get("success"));
        assertEquals(0, report.get("skipped"));
        assertEquals(1, report.get("totalProcessed"));
        assertEquals(savedUser, mockStaff.getUser());
    }

    @Test
    void provisionAccounts_NoRolesFound_ThrowsException() {
        AccountProvisionDto dto = new AccountProvisionDto();
        dto.setRoleIds(List.of(1L));
        when(roleRepository.findAllById(dto.getRoleIds())).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> staffService.provisionAccounts(dto));
    }

    @Test
    void provisionAccounts_SkipExistingEmail() {
        AccountProvisionDto dto = new AccountProvisionDto();
        dto.setRoleIds(List.of(1L));
        dto.setStaffIds(List.of(1L));

        when(roleRepository.findAllById(dto.getRoleIds())).thenReturn(List.of(new Role()));
        when(staffRepository.findAllById(dto.getStaffIds())).thenReturn(List.of(mockStaff));
        when(userRepository.existsByEmail("it260001@school.edu.vn")).thenReturn(true);

        Map<String, Object> report = staffService.provisionAccounts(dto);

        assertEquals(0, report.get("success"));
        assertEquals(1, report.get("skipped"));
    }

    @Test
    void provisionAccounts_SkipAlreadyHasUser() {
        AccountProvisionDto dto = new AccountProvisionDto();
        dto.setRoleIds(List.of(1L));
        dto.setStaffIds(List.of(1L));

        mockStaff.setUser(mockUser);

        when(roleRepository.findAllById(dto.getRoleIds())).thenReturn(List.of(new Role()));
        when(staffRepository.findAllById(dto.getStaffIds())).thenReturn(List.of(mockStaff));

        Map<String, Object> report = staffService.provisionAccounts(dto);

        assertEquals(0, report.get("success"));
        assertEquals(1, report.get("skipped"));
    }

    @Test
    void getMyProfile_Success() {
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        StaffDto result = staffService.getMyProfile(1L);
        assertEquals("IT260001", result.getStaffCode());
    }

    @Test
    void getMyProfile_NotFound_ThrowsException() {
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> staffService.getMyProfile(1L));
    }
}
