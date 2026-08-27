package com.lms.education.module.notification.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.notification.dto.ClassAnnouncementDto;
import com.lms.education.module.notification.entity.ClassAnnouncement;
import com.lms.education.module.notification.repository.ClassAnnouncementRepository;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.UserRepository;
import com.lms.education.service.MinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ClassAnnouncementServiceImplTest {

    @Mock
    private ClassAnnouncementRepository classAnnouncementRepository;
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
    private MinioStorageService minioStorageService;

    @InjectMocks
    private ClassAnnouncementServiceImpl announcementService;

    private Classes mockClass;
    private User mockUser;
    private Staff mockStaff;
    private ClassAnnouncement mockAnnouncement;
    private ClassAnnouncementDto mockDto;

    @BeforeEach
    void setUp() {
        mockClass = new Classes();
        mockClass.setId(10L);
        mockClass.setName("Test Class");

        Role teacherRole = new Role();
        teacherRole.setId(2L);
        teacherRole.setName("ROLE_TEACHER");

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("teacher@test.com");
        mockUser.setRoles(Set.of(teacherRole));

        mockStaff = new Staff();
        mockStaff.setId(100L);
        mockStaff.setUser(mockUser);

        mockAnnouncement = new ClassAnnouncement();
        mockAnnouncement.setId(5L);
        mockAnnouncement.setClasses(mockClass);
        mockAnnouncement.setCreatedBy(mockUser);
        mockAnnouncement.setTitle("Test Title");
        mockAnnouncement.setContent("Test Content");
        mockAnnouncement.setIsPinned(false);
        mockAnnouncement.setHasAttachment(false);

        mockDto = ClassAnnouncementDto.builder()
                .classId(10L)
                .createdById(1L)
                .title("Test Title")
                .content("Test Content")
                .build();
                
        // Setup SecurityContext
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("teacher@test.com");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))).when(authentication).getAuthorities();
        
        when(minioStorageService.getFileUrl(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void createAnnouncement_Success() {
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(true);
        when(classAnnouncementRepository.save(any(ClassAnnouncement.class))).thenReturn(mockAnnouncement);

        ClassAnnouncementDto result = announcementService.createAnnouncement(mockDto);

        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
    }
    
    @Test
    void createAnnouncement_ClassNotFound_ThrowsException() {
        when(classesRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> announcementService.createAnnouncement(mockDto));
    }
    
    @Test
    void createAnnouncement_NotAuthorized_ThrowsException() {
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(false);
        when(teachingSubstitutionRepository.isTeacherSubstitutingForClass(10L, 1L, 100L)).thenReturn(false);

        assertThrows(OperationNotPermittedException.class, () -> announcementService.createAnnouncement(mockDto));
    }
    
    @Test
    void createAnnouncement_AdminRole_Success() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();
        
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(classAnnouncementRepository.save(any(ClassAnnouncement.class))).thenReturn(mockAnnouncement);

        ClassAnnouncementDto result = announcementService.createAnnouncement(mockDto);

        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
    }

    @Test
    void createAnnouncementWithFile_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "dummy".getBytes());

        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(true);
        when(minioStorageService.uploadFileKeepName(any())).thenReturn("url/test.txt");
        
        mockAnnouncement.setHasAttachment(true);
        mockAnnouncement.setAttachmentUrl("url/test.txt");
        when(classAnnouncementRepository.save(any(ClassAnnouncement.class))).thenReturn(mockAnnouncement);

        ClassAnnouncementDto result = announcementService.createAnnouncementWithFile(10L, "Title", "Content", true, file);

        assertNotNull(result);
        assertTrue(result.getHasAttachment());
    }

    @Test
    void updateAnnouncement_Success() {
        when(classAnnouncementRepository.findById(5L)).thenReturn(Optional.of(mockAnnouncement));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(true);
        when(classAnnouncementRepository.save(any(ClassAnnouncement.class))).thenReturn(mockAnnouncement);

        ClassAnnouncementDto result = announcementService.updateAnnouncement(5L, "New Title", "New Content", true, false, null);

        assertNotNull(result);
        assertEquals("New Title", mockAnnouncement.getTitle());
    }

    @Test
    void togglePin_Success() {
        when(classAnnouncementRepository.findById(5L)).thenReturn(Optional.of(mockAnnouncement));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(true);
        when(classAnnouncementRepository.save(any(ClassAnnouncement.class))).thenReturn(mockAnnouncement);

        ClassAnnouncementDto result = announcementService.togglePin(5L, true);

        assertNotNull(result);
        assertTrue(mockAnnouncement.getIsPinned());
    }

    @Test
    void getAnnouncementsByClass_Success() {
        when(classesRepository.existsById(10L)).thenReturn(true);
        when(classAnnouncementRepository.findByClassesIdOrderByIsPinnedDescCreatedAtDesc(10L)).thenReturn(List.of(mockAnnouncement));

        List<ClassAnnouncementDto> result = announcementService.getAnnouncementsByClass(10L);

        assertEquals(1, result.size());
    }

    @Test
    void getAnnouncementById_Success() {
        when(classAnnouncementRepository.findById(5L)).thenReturn(Optional.of(mockAnnouncement));

        ClassAnnouncementDto result = announcementService.getAnnouncementById(5L);

        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
    }


    @Test
    void deleteAnnouncement_Success() {
        when(classAnnouncementRepository.findById(5L)).thenReturn(Optional.of(mockAnnouncement));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(true);

        announcementService.deleteAnnouncement(5L);

        verify(classAnnouncementRepository).delete(mockAnnouncement);
    }

    @Test
    void createAnnouncementWithFile_EmptyFile_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[0]);
        assertThrows(OperationNotPermittedException.class, () -> announcementService.createAnnouncementWithFile(10L, "Title", "Content", true, file));
    }

    @Test
    void createAnnouncementWithFile_UserNull_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "dummy".getBytes());
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.empty()); // simulate user not found
        
        assertThrows(ResourceNotFoundException.class, () -> announcementService.createAnnouncementWithFile(10L, "Title", "Content", true, file));
    }

    @Test
    void updateAnnouncement_RemoveAttachment_Success() {
        mockAnnouncement.setHasAttachment(true);
        mockAnnouncement.setAttachmentUrl("url/old.txt");
        
        when(classAnnouncementRepository.findById(5L)).thenReturn(Optional.of(mockAnnouncement));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(true);
        when(classAnnouncementRepository.save(any(ClassAnnouncement.class))).thenAnswer(i -> i.getArgument(0));

        ClassAnnouncementDto result = announcementService.updateAnnouncement(5L, "New Title", "New Content", true, true, null);

        assertNotNull(result);
        assertFalse(result.getHasAttachment());
        assertNull(result.getAttachmentUrl());
        verify(minioStorageService).deleteFile("old.txt");
    }

    @Test
    void updateAnnouncement_ReplaceAttachment_Success() {
        mockAnnouncement.setHasAttachment(true);
        mockAnnouncement.setAttachmentUrl("url/old.txt");
        
        MockMultipartFile newFile = new MockMultipartFile("file", "new.txt", "text/plain", "dummy".getBytes());
        
        when(classAnnouncementRepository.findById(5L)).thenReturn(Optional.of(mockAnnouncement));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(true);
        when(minioStorageService.uploadFileKeepName(any())).thenReturn("url/new.txt");
        when(classAnnouncementRepository.save(any(ClassAnnouncement.class))).thenAnswer(i -> i.getArgument(0));

        ClassAnnouncementDto result = announcementService.updateAnnouncement(5L, null, null, null, false, newFile);

        assertNotNull(result);
        assertTrue(result.getHasAttachment());
        assertEquals("url/new.txt", result.getAttachmentUrl());
        verify(minioStorageService).deleteFile("old.txt");
        verify(minioStorageService).uploadFileKeepName(newFile);
    }
    
    @Test
    void getAnnouncementsByClassPaged_Success() {
        when(classesRepository.existsById(10L)).thenReturn(true);
        Page<ClassAnnouncement> page = new PageImpl<>(List.of(mockAnnouncement));
        when(classAnnouncementRepository.findByClassesIdOrderByIsPinnedDescCreatedAtDesc(eq(10L), any(PageRequest.class))).thenReturn(page);

        Page<ClassAnnouncementDto> result = announcementService.getAnnouncementsByClassPaged(10L, 0, 10);

        assertEquals(1, result.getTotalElements());
    }
    
    @Test
    void getAnnouncementsByClassPaged_NotFound_ThrowsException() {
        when(classesRepository.existsById(10L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> announcementService.getAnnouncementsByClassPaged(10L, 0, 10));
    }

    // --- Additional Edge Case Tests for Branch Coverage ---

    @Test
    void createAnnouncement_WithOptionalFieldsNull() {
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(true);
        when(classAnnouncementRepository.save(any(ClassAnnouncement.class))).thenAnswer(i -> i.getArgument(0));

        mockDto.setAttachmentUrl(null);
        mockDto.setIsPinned(null);

        ClassAnnouncementDto result = announcementService.createAnnouncement(mockDto);

        assertNotNull(result);
        assertFalse(result.getIsPinned());
        assertFalse(result.getHasAttachment());
    }
    
    @Test
    void createAnnouncement_WithEmptyAttachmentUrl() {
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(true);
        when(classAnnouncementRepository.save(any(ClassAnnouncement.class))).thenAnswer(i -> i.getArgument(0));

        mockDto.setAttachmentUrl("   ");
        mockDto.setIsPinned(true);

        ClassAnnouncementDto result = announcementService.createAnnouncement(mockDto);

        assertNotNull(result);
        assertTrue(result.getIsPinned());
        assertFalse(result.getHasAttachment());
    }

    @Test
    void createAnnouncement_UserNotFound_ThrowsException() {
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(userRepository.findById(1L)).thenReturn(Optional.empty()); // null resolved user

        assertThrows(ResourceNotFoundException.class, () -> announcementService.createAnnouncement(mockDto));
    }

    @Test
    void createAnnouncementWithFile_NullFile_ThrowsException() {
        assertThrows(OperationNotPermittedException.class, () -> announcementService.createAnnouncementWithFile(10L, "Title", "Content", true, null));
    }
    
    @Test
    void createAnnouncementWithFile_IsPinnedNull() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "dummy".getBytes());
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(true);
        when(minioStorageService.uploadFileKeepName(any())).thenReturn("url/test.txt");
        when(classAnnouncementRepository.save(any(ClassAnnouncement.class))).thenAnswer(i -> i.getArgument(0));

        ClassAnnouncementDto result = announcementService.createAnnouncementWithFile(10L, "Title", "Content", null, file);

        assertNotNull(result);
        assertFalse(result.getIsPinned());
    }

    @Test
    void updateAnnouncement_NullFields() {
        when(classAnnouncementRepository.findById(5L)).thenReturn(Optional.of(mockAnnouncement));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(true);
        when(classAnnouncementRepository.save(any(ClassAnnouncement.class))).thenAnswer(i -> i.getArgument(0));

        ClassAnnouncementDto result = announcementService.updateAnnouncement(5L, "   ", null, null, null, null);

        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
    }

    @Test
    void updateAnnouncement_RemoveAttachmentException() {
        mockAnnouncement.setHasAttachment(true);
        mockAnnouncement.setAttachmentUrl("http/url/old.txt");
        
        when(classAnnouncementRepository.findById(5L)).thenReturn(Optional.of(mockAnnouncement));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(true);
        when(classAnnouncementRepository.save(any(ClassAnnouncement.class))).thenAnswer(i -> i.getArgument(0));
        
        doThrow(new RuntimeException("Minio error")).when(minioStorageService).deleteFile("old.txt");

        ClassAnnouncementDto result = announcementService.updateAnnouncement(5L, null, null, null, true, null);

        assertNotNull(result);
        assertFalse(result.getHasAttachment());
        assertNull(result.getAttachmentUrl());
    }
    
    @Test
    void updateAnnouncement_ReplaceAttachmentException() {
        mockAnnouncement.setHasAttachment(true);
        mockAnnouncement.setAttachmentUrl("http/url/old.txt");
        
        MockMultipartFile newFile = new MockMultipartFile("file", "new.txt", "text/plain", "dummy".getBytes());
        
        when(classAnnouncementRepository.findById(5L)).thenReturn(Optional.of(mockAnnouncement));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(1L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 1L, 100L)).thenReturn(true);
        when(classAnnouncementRepository.save(any(ClassAnnouncement.class))).thenAnswer(i -> i.getArgument(0));
        
        doThrow(new RuntimeException("Minio error")).when(minioStorageService).deleteFile("old.txt");
        when(minioStorageService.uploadFileKeepName(any())).thenReturn("url/new.txt");

        ClassAnnouncementDto result = announcementService.updateAnnouncement(5L, null, null, null, false, newFile);

        assertNotNull(result);
        assertTrue(result.getHasAttachment());
        assertEquals("url/new.txt", result.getAttachmentUrl()); // it calls minioStorageService.getFileUrl in toDto if it doesn't start with http
    }

    @Test
    void checkTeacherClassPermission_NoUser() {
        // Clear authentication
        SecurityContextHolder.getContext().setAuthentication(null);
        
        when(classAnnouncementRepository.findById(5L)).thenReturn(Optional.of(mockAnnouncement));
        
        assertThrows(OperationNotPermittedException.class, () -> announcementService.deleteAnnouncement(5L));
    }

    @Test
    void isAcademicDepartmentUser_ByRoleName() {
        Role academicRole = new Role();
        academicRole.setId(3L);
        academicRole.setName("ROLE_ACADEMIC");
        mockUser.setRoles(Set.of(academicRole));

        when(classAnnouncementRepository.findById(5L)).thenReturn(Optional.of(mockAnnouncement));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        // No assignment needed due to academic role

        announcementService.deleteAnnouncement(5L);
        verify(classAnnouncementRepository).delete(mockAnnouncement);
    }
    
    @Test
    void isAcademicDepartmentUser_ByRoleId() {
        Role academicRole = new Role();
        academicRole.setId(8L);
        academicRole.setName("SOME_ROLE");
        mockUser.setRoles(Set.of(academicRole));

        when(classAnnouncementRepository.findById(5L)).thenReturn(Optional.of(mockAnnouncement));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        // No assignment needed due to role ID 8

        announcementService.deleteAnnouncement(5L);
        verify(classAnnouncementRepository).delete(mockAnnouncement);
    }

    @Test
    void toDto_HttpAttachmentUrl_NullClassNullAuthor() {
        ClassAnnouncement announcement = new ClassAnnouncement();
        announcement.setId(99L);
        announcement.setClasses(null);
        announcement.setCreatedBy(null);
        announcement.setAttachmentUrl("http://example.com/file.txt");
        announcement.setHasAttachment(true);
        announcement.setIsPinned(true);

        when(classAnnouncementRepository.findById(99L)).thenReturn(Optional.of(announcement));

        ClassAnnouncementDto result = announcementService.getAnnouncementById(99L);

        assertNotNull(result);
        assertNull(result.getClassId());
        assertNull(result.getCreatedById());
        assertEquals("http://example.com/file.txt", result.getAttachmentUrl());
    }
}
