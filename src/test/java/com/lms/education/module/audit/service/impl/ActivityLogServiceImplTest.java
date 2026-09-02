package com.lms.education.module.audit.service.impl;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.audit.dto.ActivityLogDto;
import com.lms.education.module.audit.entity.ActivityLog;
import com.lms.education.module.audit.repository.ActivityLogRepository;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.UserRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ActivityLogServiceImplTest {

    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ActivityLogServiceImpl activityLogService;

    private ActivityLog mockLog;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");

        mockLog = new ActivityLog();
        mockLog.setId(100L);
        mockLog.setUserId(1L);
        mockLog.setActorName("test@example.com");
        mockLog.setModule("TEST_MODULE");
        mockLog.setAction("CREATE");
        mockLog.setStatus(ActivityLog.LogStatus.success);
    }

    @Test
    void logAction_Success_AuthenticatedUser() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("test@example.com");
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        activityLogService.logAction("TEST_MODULE", "CREATE", "TEST_TYPE", "1", null, null, "details", ActivityLog.LogStatus.success);

        verify(activityLogRepository).save(any(ActivityLog.class));
        
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void logAction_Success_AnonymousUser() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        activityLogService.logAction("TEST_MODULE", "CREATE", "TEST_TYPE", "1", null, null, "details", ActivityLog.LogStatus.success);

        verify(activityLogRepository).save(any(ActivityLog.class));
    }
    
    @Test
    void logAction_Exception_HandledGracefully() {
        SecurityContextHolder.clearContext();
        when(activityLogRepository.save(any(ActivityLog.class))).thenThrow(new RuntimeException("DB Error"));

        // Should not throw exception
        assertDoesNotThrow(() -> {
            activityLogService.logAction("TEST_MODULE", "CREATE", "TEST_TYPE", "1", null, null, "details", ActivityLog.LogStatus.success);
        });
    }

    @Test
    void searchAndFilterLogs_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ActivityLog> page = new PageImpl<>(List.of(mockLog));
        
        when(activityLogRepository.searchAndFilterLogs(null, null, null, null, null, null, pageable)).thenReturn(page);

        Page<ActivityLogDto> result = activityLogService.searchAndFilterLogs(null, null, null, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getLogsByUserId_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ActivityLog> page = new PageImpl<>(List.of(mockLog));
        
        when(activityLogRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);

        Page<ActivityLogDto> result = activityLogService.getLogsByUserId(1L, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getById_Success() {
        when(activityLogRepository.findById(100L)).thenReturn(Optional.of(mockLog));

        ActivityLogDto result = activityLogService.getById(100L);

        assertNotNull(result);
        assertEquals("TEST_MODULE", result.getModule());
    }
    
    @Test
    void getById_NotFound_ThrowsException() {
        when(activityLogRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> activityLogService.getById(100L));
    }
}
