package com.lms.education.module.user.service.impl;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.UserDto;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.RoleRepository;
import com.lms.education.module.user.repository.StaffRepository;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User mockUser;
    private UserDto mockUserDto;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@gmail.com");
        mockUser.setFullName("Test User");
        mockUser.setPassword("encodedPassword");
        mockUser.setStatus("ACTIVE");
        mockUser.setRoles(new HashSet<>());

        mockUserDto = UserDto.builder()
                .id(1L)
                .email("test@gmail.com")
                .fullName("Test User")
                .build();
    }

    @Test
    void findByEmail_Success() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        Optional<User> result = userService.findByEmail("test@gmail.com");
        assertTrue(result.isPresent());
        assertEquals("test@gmail.com", result.get().getEmail());
    }

    @Test
    void updateRefreshToken_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        Instant expiryDate = Instant.now().plusSeconds(3600);
        
        userService.updateRefreshToken(1L, "newToken", expiryDate);
        
        verify(userRepository).save(mockUser);
        assertEquals("newToken", mockUser.getRefreshToken());
        assertEquals(expiryDate, mockUser.getExpiryDate());
    }

    @Test
    void updateRefreshToken_UserNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> 
            userService.updateRefreshToken(1L, "token", Instant.now()));
    }

    @Test
    void findByRefreshToken_Success() {
        when(userRepository.findByRefreshToken("token")).thenReturn(Optional.of(mockUser));
        Optional<User> result = userService.findByRefreshToken("token");
        assertTrue(result.isPresent());
    }

    @Test
    void deleteRefreshToken_Success() {
        when(userRepository.findByRefreshToken("token")).thenReturn(Optional.of(mockUser));
        userService.deleteRefreshToken("token");
        verify(userRepository).save(mockUser);
        assertNull(mockUser.getRefreshToken());
        assertNull(mockUser.getExpiryDate());
    }

    @Test
    void createUser_Success() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserDto result = userService.createUser(mockUserDto);

        assertNotNull(result);
        assertEquals("test@gmail.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserStatus_Active_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        userService.updateUserStatus(1L, "ACTIVE");
        assertEquals("ACTIVE", mockUser.getStatus());
        verify(userRepository).save(mockUser);
    }

    @Test
    void updateUserStatus_Locked_ClearsToken() {
        mockUser.setRefreshToken("token");
        mockUser.setExpiryDate(Instant.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        
        userService.updateUserStatus(1L, "LOCKED");
        
        assertEquals("LOCKED", mockUser.getStatus());
        assertNull(mockUser.getRefreshToken());
        assertNull(mockUser.getExpiryDate());
        verify(userRepository).save(mockUser);
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        UserDto result = userService.getUserById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getUserById_UserNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(1L));
    }

    @Test
    void getAllUsers_WithKeyword() {
        Page<User> page = new PageImpl<>(List.of(mockUser));
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase("test", "test", pageable))
            .thenReturn(page);

        Page<UserDto> result = userService.getAllUsers("test", pageable);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase("test", "test", pageable);
    }

    @Test
    void getAllUsers_WithoutKeyword() {
        Page<User> page = new PageImpl<>(List.of(mockUser));
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<UserDto> result = userService.getAllUsers("  ", pageable);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void updateUser_WithFullNameAndRoles() {
        Role role = new Role();
        role.setName("ROLE_USER");
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserDto result = userService.updateUser(1L, "New Name", List.of("ROLE_USER"));

        assertEquals("New Name", mockUser.getFullName());
        assertEquals(1, mockUser.getRoles().size());
        verify(userRepository).save(mockUser);
    }

    @Test
    void updateUserRoles_RoleNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(roleRepository.findByName("ROLE_INVALID")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            userService.updateUserRoles(1L, List.of("ROLE_INVALID")));
    }
    
    @Test
    void updateUserRoles_EmptyRoles() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserDto result = userService.updateUserRoles(1L, Collections.emptyList());

        assertTrue(mockUser.getRoles().isEmpty());
        verify(userRepository).save(mockUser);
    }

    @Test
    void resetPassword_Student() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        Student student = new Student();
        student.setStudentCode("STU001");
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode("STU001")).thenReturn("encodedSTU001");

        userService.resetPassword(1L, "any_new_pass");

        assertEquals("encodedSTU001", mockUser.getPassword());
        assertNull(mockUser.getRefreshToken());
        verify(userRepository).save(mockUser);
    }

    @Test
    void resetPassword_Staff() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.empty());
        Staff staff = new Staff();
        staff.setStaffCode("STAFF01");
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(staff));
        when(passwordEncoder.encode("STAFF01")).thenReturn("encodedSTAFF01");

        userService.resetPassword(1L, "any_new_pass");

        assertEquals("encodedSTAFF01", mockUser.getPassword());
        verify(userRepository).save(mockUser);
    }

    @Test
    void resetPassword_Fallback() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("encoded123456");

        userService.resetPassword(1L, "any");

        assertEquals("encoded123456", mockUser.getPassword());
        verify(userRepository).save(mockUser);
    }

    @Test
    void changePassword_Success() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@gmail.com");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("oldPass", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");

        userService.changePassword("oldPass", "newPass");

        assertEquals("encodedNewPass", mockUser.getPassword());
        assertNull(mockUser.getRefreshToken());
        verify(userRepository).save(mockUser);
    }

    @Test
    void changePassword_BadCredentials_ThrowsException() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@gmail.com");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongOldPass", "encodedPassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> 
            userService.changePassword("wrongOldPass", "newPass"));
    }

    @Test
    void deleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        
        Student student = new Student();
        student.setUser(mockUser);
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.of(student));
        
        Staff staff = new Staff();
        staff.setUser(mockUser);
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(staff));

        userService.deleteUser(1L);

        assertNull(student.getUser());
        verify(studentRepository).save(student);
        assertNull(staff.getUser());
        verify(staffRepository).save(staff);
        assertNull(mockUser.getRefreshToken());
        verify(userRepository).delete(mockUser);
    }
}
