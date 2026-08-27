package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.lms.dto.LearningMaterialDto;
import com.lms.education.module.lms.entity.LearningMaterial;
import com.lms.education.module.lms.entity.Lesson;
import com.lms.education.module.lms.repository.LearningMaterialRepository;
import com.lms.education.module.lms.repository.LessonRepository;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.UserRepository;
import com.lms.education.service.MinioStorageService;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LearningMaterialServiceImplTest {

    @Mock
    private LearningMaterialRepository learningMaterialRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseRepository courseRepository;
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
    private LearningMaterialServiceImpl learningMaterialService;

    private LearningMaterial mockMaterial;
    private LearningMaterialDto mockDto;
    private Lesson mockLesson;
    private Course mockCourse;
    private Classes mockClass;
    private User mockUser;
    private Staff mockStaff;
    private MultipartFile mockFile;
    
    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        mockClass = new Classes();
        mockClass.setId(10L);

        mockLesson = new Lesson();
        mockLesson.setId(1L);
        mockLesson.setClasses(mockClass);
        
        mockCourse = new Course();
        mockCourse.setId(20L);

        mockUser = new User();
        mockUser.setId(100L);
        mockUser.setEmail("teacher@test.com");
        
        mockStaff = new Staff();
        mockStaff.setId(200L);

        mockMaterial = new LearningMaterial();
        mockMaterial.setId(1L);
        mockMaterial.setLesson(mockLesson);
        mockMaterial.setTitle("Material 1");
        mockMaterial.setMaterialScope("LESSON");
        mockMaterial.setSourceType("MINIO");
        mockMaterial.setResourceUrl("path/to/file.pdf");
        mockMaterial.setIsOfficial(false);
        mockMaterial.setIsRagEnabled(false);
        mockMaterial.setIndexingStatus("NOT_INDEXED");

        mockDto = LearningMaterialDto.builder()
                .lessonId(1L)
                .title("Material 1")
                .resourceUrl("http://example.com")
                .build();
                
        mockFile = mock(MultipartFile.class);

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
    void createWithFile_Success_Lesson() {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("file.pdf");
        when(mockFile.getSize()).thenReturn(1024L);
        
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(minioStorageService.uploadFile(mockFile)).thenReturn("path/to/file.pdf");
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenReturn(mockMaterial);

        LearningMaterialDto result = learningMaterialService.createWithFile(null, 1L, "Test Title", "DOCUMENT", 1, mockFile);

        assertNotNull(result);
        assertEquals("Material 1", result.getTitle());
    }

    @Test
    void createWithFile_Success_Course() {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("file.pdf");
        when(mockFile.getSize()).thenReturn(1024L);
        
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(courseRepository.findById(20L)).thenReturn(Optional.of(mockCourse));
        when(minioStorageService.uploadFile(mockFile)).thenReturn("path/to/file.pdf");
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenReturn(mockMaterial);

        LearningMaterialDto result = learningMaterialService.createWithFile(20L, null, "Test Title", "DOCUMENT", 1, mockFile);

        assertNotNull(result);
    }

    @Test
    void createWithFile_EmptyFile_ThrowsException() {
        when(mockFile.isEmpty()).thenReturn(true);
        assertThrows(OperationNotPermittedException.class, () -> 
                learningMaterialService.createWithFile(null, 1L, "Test Title", "DOCUMENT", 1, mockFile));
    }

    @Test
    void createWithFile_NoCourseOrLesson_ThrowsException() {
        when(mockFile.isEmpty()).thenReturn(false);
        assertThrows(OperationNotPermittedException.class, () -> 
                learningMaterialService.createWithFile(null, null, "Test Title", "DOCUMENT", 1, mockFile));
    }

    @Test
    void createWithFile_BothCourseAndLesson_ThrowsException() {
        when(mockFile.isEmpty()).thenReturn(false);
        assertThrows(OperationNotPermittedException.class, () -> 
                learningMaterialService.createWithFile(20L, 1L, "Test Title", "DOCUMENT", 1, mockFile));
    }

    @Test
    void createExternalLink_Success_Lesson() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenReturn(mockMaterial);

        LearningMaterialDto result = learningMaterialService.createExternalLink(mockDto);

        assertNotNull(result);
    }
    
    @Test
    void createExternalLink_EmptyUrl_ThrowsException() {
        mockDto.setResourceUrl("");
        assertThrows(OperationNotPermittedException.class, () -> learningMaterialService.createExternalLink(mockDto));
    }

    @Test
    void update_Success_UpdateTitle() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenReturn(mockMaterial);

        mockDto.setTitle("Updated Title");
        LearningMaterialDto result = learningMaterialService.update(1L, mockDto, null);

        assertNotNull(result);
        verify(learningMaterialRepository).save(mockMaterial);
    }
    
    @Test
    void update_Success_ChangeToCourse() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission(); // For original lesson permission
        when(courseRepository.findById(20L)).thenReturn(Optional.of(mockCourse));
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenReturn(mockMaterial);

        mockDto.setLessonId(null);
        mockDto.setCourseId(20L);
        LearningMaterialDto result = learningMaterialService.update(1L, mockDto, null);

        assertNotNull(result);
    }

    @Test
    void update_MaterialNotFound_ThrowsException() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> learningMaterialService.update(1L, mockDto, null));
    }

    @Test
    void delete_Success_LessonMaterial() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        setupTeacherPermission();

        learningMaterialService.delete(1L);

        verify(minioStorageService).deleteFile("path/to/file.pdf");
        verify(learningMaterialRepository).delete(mockMaterial);
    }
    
    @Test
    void delete_MaterialNotFound_ThrowsException() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> learningMaterialService.delete(1L));
    }

    @Test
    void getById_Success() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        LearningMaterialDto result = learningMaterialService.getById(1L);
        assertEquals("Material 1", result.getTitle());
    }

    @Test
    void getByLessonId_Success() {
        when(learningMaterialRepository.findByLessonIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of(mockMaterial));
        List<LearningMaterialDto> result = learningMaterialService.getByLessonId(1L);
        assertEquals(1, result.size());
    }
    
    @Test
    void getByCourseId_Success() {
        when(learningMaterialRepository.findByCourseIdOrderByDisplayOrderAsc(20L)).thenReturn(List.of(mockMaterial));
        List<LearningMaterialDto> result = learningMaterialService.getByCourseId(20L);
        assertEquals(1, result.size());
    }
    
    @Test
    void getByClassId_Success() {
        when(learningMaterialRepository.findByLessonClassesIdOrderByDisplayOrderAsc(10L)).thenReturn(List.of(mockMaterial));
        List<LearningMaterialDto> result = learningMaterialService.getByClassId(10L);
        assertEquals(1, result.size());
    }

    @Test
    void getAll_Success_WithKeyword() {
        Page<LearningMaterial> page = new PageImpl<>(List.of(mockMaterial));
        Pageable pageable = PageRequest.of(0, 10);
        when(learningMaterialRepository.searchMaterials("Material", pageable)).thenReturn(page);

        Page<LearningMaterialDto> result = learningMaterialService.getAll("Material", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAll_Success_EmptyKeyword() {
        Page<LearningMaterial> page = new PageImpl<>(List.of(mockMaterial));
        Pageable pageable = PageRequest.of(0, 10);
        when(learningMaterialRepository.findAll(pageable)).thenReturn(page);

        Page<LearningMaterialDto> result = learningMaterialService.getAll("", pageable);

        assertEquals(1, result.getTotalElements());
    }
    
    @Test
    void createWithFile_PermissionDenied_ThrowsException() {
        when(mockFile.isEmpty()).thenReturn(false);
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(staffRepository.findByUserId(100L)).thenReturn(Optional.of(mockStaff));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(10L, 100L, 200L)).thenReturn(false);
        when(teachingSubstitutionRepository.isTeacherSubstitutingForClass(10L, 100L, 200L)).thenReturn(false);

        assertThrows(OperationNotPermittedException.class, () -> 
                learningMaterialService.createWithFile(null, 1L, "Test Title", "DOCUMENT", 1, mockFile));
    }

    @Test
    void update_ChangeIsOfficial_NotAcademic_ThrowsException() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setIsOfficial(false);
        mockDto.setIsOfficial(true); // Attempt to change
        
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();

        assertThrows(OperationNotPermittedException.class, () -> learningMaterialService.update(1L, mockDto, null));
    }
    
    @Test
    void update_ChangeIsRagEnabled_NotAcademic_ThrowsException() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setIsRagEnabled(false);
        mockDto.setIsRagEnabled(true); // Attempt to change
        
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();

        assertThrows(OperationNotPermittedException.class, () -> learningMaterialService.update(1L, mockDto, null));
    }
    
    @Test
    void update_WithNewFile_ReplacingMinioSource() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setSourceType("MINIO");
        mockMaterial.setResourceUrl("old/path.pdf");
        
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(2048L);
        when(minioStorageService.uploadFile(mockFile)).thenReturn("new/path.pdf");
        when(learningMaterialRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        LearningMaterialDto result = learningMaterialService.update(1L, mockDto, mockFile);
        
        verify(minioStorageService).deleteFile("old/path.pdf");
        verify(minioStorageService).uploadFile(mockFile);
        assertEquals("new/path.pdf", result.getResourceUrl());
    }
    
    @Test
    void update_WithExternalLink_ReplacingMinioSource() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setSourceType("MINIO");
        mockMaterial.setResourceUrl("old/path.pdf");
        
        mockDto.setSourceType("EXTERNAL");
        mockDto.setResourceUrl("http://newlink.com");
        
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(learningMaterialRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        LearningMaterialDto result = learningMaterialService.update(1L, mockDto, null);
        
        verify(minioStorageService).deleteFile("old/path.pdf");
        assertEquals("EXTERNAL", result.getSourceType());
        assertEquals("http://newlink.com", result.getResourceUrl());
    }

    // --- Extra tests for branches ---

    @Test
    void createWithFile_NullFields_UsesDefaults() {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("original.pdf");
        when(mockFile.getSize()).thenReturn(1024L);

        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        when(minioStorageService.uploadFile(mockFile)).thenReturn("path.pdf");
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenAnswer(i -> i.getArgument(0));

        // title=null, materialType=null, displayOrder=null
        LearningMaterialDto result = learningMaterialService.createWithFile(null, 1L, null, null, null, mockFile);

        assertEquals("original.pdf", result.getTitle());
        assertEquals("DOCUMENT", result.getMaterialType());
        assertEquals(0, result.getDisplayOrder());
    }

    @Test
    void createExternalLink_Success_Course() {
        mockDto.setCourseId(20L);
        mockDto.setLessonId(null);
        mockDto.setMaterialType(null);
        mockDto.setDisplayOrder(null);
        mockDto.setResourceUrl("http://example.com");
        
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));
        when(courseRepository.findById(20L)).thenReturn(Optional.of(mockCourse));
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenAnswer(i -> {
            LearningMaterial saved = i.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        LearningMaterialDto result = learningMaterialService.createExternalLink(mockDto);

        assertNotNull(result);
        assertEquals("EXTERNAL_LINK", result.getMaterialType());
        assertEquals(0, result.getDisplayOrder());
        assertEquals("COURSE", result.getMaterialScope());
    }

    @Test
    void createExternalLink_NoCourseOrLesson_ThrowsException() {
        mockDto.setCourseId(null);
        mockDto.setLessonId(null);
        mockDto.setResourceUrl("http://link.com");
        assertThrows(OperationNotPermittedException.class, () -> learningMaterialService.createExternalLink(mockDto));
    }

    @Test
    void createExternalLink_BothCourseAndLesson_ThrowsException() {
        mockDto.setCourseId(20L);
        mockDto.setLessonId(1L);
        mockDto.setResourceUrl("http://link.com");
        assertThrows(OperationNotPermittedException.class, () -> learningMaterialService.createExternalLink(mockDto));
    }

    @Test
    void update_ChangeToLesson() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setMaterialScope("COURSE");
        mockMaterial.setCourse(mockCourse);
        mockMaterial.setLesson(null);

        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission(); // For new lesson permission
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenAnswer(i -> i.getArgument(0));

        mockDto.setCourseId(null);
        mockDto.setLessonId(1L);
        mockDto.setTitle(""); // empty title should not change
        mockDto.setMaterialType("  "); // empty type should not change
        LearningMaterialDto result = learningMaterialService.update(1L, mockDto, null);

        assertNotNull(result);
        assertEquals("LESSON", result.getMaterialScope());
    }

    @Test
    void update_IsRagEnabled_ByAcademic() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setIsRagEnabled(false);
        mockMaterial.setIndexingStatus("NOT_INDEXED");
        
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission(); // Added to pass teacher check
        
        // Mock academic user
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        mockUser.setRoles(java.util.Set.of(adminRole));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));

        mockDto.setIsRagEnabled(true);
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenAnswer(i -> i.getArgument(0));

        LearningMaterialDto result = learningMaterialService.update(1L, mockDto, null);
        assertTrue(result.getIsRagEnabled());
    }

    @Test
    void update_DisableRagEnabled_ByAcademic() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setIsRagEnabled(true);
        mockMaterial.setIndexingStatus("INDEXED");
        
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission(); // Added to pass teacher check
        
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        mockUser.setRoles(java.util.Set.of(adminRole));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));

        mockDto.setIsRagEnabled(false);
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenAnswer(i -> i.getArgument(0));

        LearningMaterialDto result = learningMaterialService.update(1L, mockDto, null);
        assertFalse(result.getIsRagEnabled());
    }

    @Test
    void update_ChangeIsOfficial_ByAcademic() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setIsOfficial(false);
        
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission(); // Added to pass teacher check
        
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        mockUser.setRoles(java.util.Set.of(adminRole));
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));

        mockDto.setIsOfficial(true);
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenAnswer(i -> i.getArgument(0));

        LearningMaterialDto result = learningMaterialService.update(1L, mockDto, null);
        assertTrue(result.getIsOfficial());
    }

    @Test
    void update_ChangeExternalToMinio() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setSourceType("EXTERNAL");
        mockMaterial.setResourceUrl("http://oldlink.com");
        
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(mockLesson));
        setupTeacherPermission();
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(2048L);
        when(minioStorageService.uploadFile(mockFile)).thenReturn("new/path.pdf");
        when(learningMaterialRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        LearningMaterialDto result = learningMaterialService.update(1L, mockDto, mockFile);
        
        assertEquals("MINIO", result.getSourceType());
        assertEquals("new/path.pdf", result.getResourceUrl());
    }
    @Test
    void createExternalLink_WithCourseIdAndLessonId_ThrowsException() {
        LearningMaterialDto externalDto = LearningMaterialDto.builder()
                .resourceUrl("http://example.com")
                .courseId(10L)
                .lessonId(20L)
                .build();
        assertThrows(OperationNotPermittedException.class, () -> learningMaterialService.createExternalLink(externalDto));
    }

    @Test
    void update_CourseMaterial_Success() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(mockCourse));
        
        LearningMaterialDto updateDto = LearningMaterialDto.builder()
                .courseId(10L)
                .title("Updated Title")
                .materialType("VIDEO")
                .displayOrder(5)
                .build();
                
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenReturn(mockMaterial);
        
        LearningMaterialDto result = learningMaterialService.update(1L, updateDto, null);
        assertNotNull(result);
        assertEquals("COURSE", mockMaterial.getMaterialScope());
    }

    @Test
    void update_LessonMaterial_Success() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        when(lessonRepository.findById(20L)).thenReturn(Optional.of(mockLesson));
        when(scheduleAssignmentRepository.isTeacherAssignedToClass(any(), any(), any())).thenReturn(true);
        
        LearningMaterialDto updateDto = LearningMaterialDto.builder()
                .lessonId(20L)
                .build();
                
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenReturn(mockMaterial);
        
        LearningMaterialDto result = learningMaterialService.update(1L, updateDto, null);
        assertNotNull(result);
        assertEquals("LESSON", mockMaterial.getMaterialScope());
    }

    @Test
    void update_IsOfficial_ByAcademicUser() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setIsOfficial(false);
        mockMaterial.setLesson(mockLesson);
        setupTeacherPermission();

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.GrantedAuthority authority = mock(org.springframework.security.core.GrantedAuthority.class);
        when(authority.getAuthority()).thenReturn("ROLE_ACADEMIC");
        doReturn(List.of(authority)).when(auth).getAuthorities();
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("teacher@test.com");
        when(auth.getName()).thenReturn("teacher@test.com");
        
        mockedSecurityContextHolder.close();
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(ctx);

        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));

        LearningMaterialDto updateDto = LearningMaterialDto.builder()
                .isOfficial(true)
                .build();
                
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenReturn(mockMaterial);
        
        LearningMaterialDto result = learningMaterialService.update(1L, updateDto, null);
        assertTrue(mockMaterial.getIsOfficial());
    }

    @Test
    void update_IsOfficial_ByNonAcademicUser_ThrowsException() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setIsOfficial(false);

        // Current auth is ROLE_TEACHER (from setUp)
        LearningMaterialDto updateDto = LearningMaterialDto.builder()
                .isOfficial(true)
                .build();
                
        assertThrows(OperationNotPermittedException.class, () -> learningMaterialService.update(1L, updateDto, null));
    }

    @Test
    void update_IsRagEnabled_ByAcademicUser() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setIsRagEnabled(false);
        mockMaterial.setIndexingStatus("NOT_INDEXED");
        mockMaterial.setLesson(mockLesson);
        setupTeacherPermission();

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.GrantedAuthority authority = mock(org.springframework.security.core.GrantedAuthority.class);
        when(authority.getAuthority()).thenReturn("ROLE_ACADEMIC");
        doReturn(List.of(authority)).when(auth).getAuthorities();
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("teacher@test.com");
        when(auth.getName()).thenReturn("teacher@test.com");
        
        mockedSecurityContextHolder.close();
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(ctx);

        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));

        LearningMaterialDto updateDto = LearningMaterialDto.builder()
                .isRagEnabled(true)
                .build();
                
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenReturn(mockMaterial);
        
        learningMaterialService.update(1L, updateDto, null);
        assertTrue(mockMaterial.getIsRagEnabled());
        assertEquals("PENDING", mockMaterial.getIndexingStatus());
    }

    @Test
    void update_IsRagEnabled_ToFalse_ByAcademicUser() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setIsRagEnabled(true);
        mockMaterial.setIndexingStatus("PENDING");
        mockMaterial.setLesson(mockLesson);
        setupTeacherPermission();

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.GrantedAuthority authority = mock(org.springframework.security.core.GrantedAuthority.class);
        when(authority.getAuthority()).thenReturn("ROLE_ACADEMIC");
        doReturn(List.of(authority)).when(auth).getAuthorities();
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("teacher@test.com");
        when(auth.getName()).thenReturn("teacher@test.com");
        
        mockedSecurityContextHolder.close();
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(ctx);

        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(mockUser));

        LearningMaterialDto updateDto = LearningMaterialDto.builder()
                .isRagEnabled(false)
                .build();
                
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenReturn(mockMaterial);
        
        learningMaterialService.update(1L, updateDto, null);
        assertFalse(mockMaterial.getIsRagEnabled());
        assertEquals("NOT_INDEXED", mockMaterial.getIndexingStatus());
    }

    @Test
    void update_IsRagEnabled_ByNonAcademicUser_ThrowsException() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setIsRagEnabled(false);

        // Current auth is ROLE_TEACHER (from setUp)
        LearningMaterialDto updateDto = LearningMaterialDto.builder()
                .isRagEnabled(true)
                .build();
                
        assertThrows(OperationNotPermittedException.class, () -> learningMaterialService.update(1L, updateDto, null));
    }

    @Test
    void update_ReplaceMinioFile() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setSourceType("MINIO");
        mockMaterial.setResourceUrl("old-file.pdf");
        
        MultipartFile newFile = mock(MultipartFile.class);
        when(newFile.isEmpty()).thenReturn(false);
        when(newFile.getSize()).thenReturn(2048L);
        when(minioStorageService.uploadFile(newFile)).thenReturn("new-file.pdf");
        
        LearningMaterialDto updateDto = LearningMaterialDto.builder().build();
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenReturn(mockMaterial);

        learningMaterialService.update(1L, updateDto, newFile);
        
        verify(minioStorageService).deleteFile("old-file.pdf");
        verify(minioStorageService).uploadFile(newFile);
        assertEquals("MINIO", mockMaterial.getSourceType());
        assertEquals("new-file.pdf", mockMaterial.getResourceUrl());
    }

    @Test
    void update_ReplaceWithExternalLink() {
        when(learningMaterialRepository.findById(1L)).thenReturn(Optional.of(mockMaterial));
        mockMaterial.setSourceType("MINIO");
        mockMaterial.setResourceUrl("old-file.pdf");
        
        LearningMaterialDto updateDto = LearningMaterialDto.builder()
                .sourceType("EXTERNAL")
                .resourceUrl("http://example.com/new")
                .build();
                
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenReturn(mockMaterial);

        learningMaterialService.update(1L, updateDto, null);
        
        verify(minioStorageService).deleteFile("old-file.pdf");
        assertEquals("EXTERNAL", mockMaterial.getSourceType());
        assertEquals("http://example.com/new", mockMaterial.getResourceUrl());
    }
}
