package com.lms.education.security;

import com.lms.education.module.academic.repository.ClassesRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LmsPermissionEvaluatorTest {

    @Mock
    private LmsSecurityService lmsSecurityService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ClassesRepository classesRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LmsPermissionEvaluator evaluator;

    private static class DummyResource implements SecurityResource {
        private final Long classId;
        private final Long courseId;

        public DummyResource(Long classId, Long courseId) {
            this.classId = classId;
            this.courseId = courseId;
        }

        @Override
        public Long extractClassId() {
            return classId;
        }

        @Override
        public Long extractCourseId() {
            return courseId;
        }
    }

    @Test
    void hasPermission_domainObject_Null() {
        assertFalse(evaluator.hasPermission(authentication, null, "READ"));
    }

    @Test
    void hasPermission_domainObject_NotSecurityResource() {
        assertFalse(evaluator.hasPermission(authentication, new Object(), "READ"));
    }

    @Test
    void hasPermission_domainObject_ClassId_Access() {
        DummyResource resource = new DummyResource(1L, null);
        when(lmsSecurityService.canAccessClass(1L)).thenReturn(true);

        assertTrue(evaluator.hasPermission(authentication, resource, "READ"));
    }

    @Test
    void hasPermission_domainObject_ClassId_Manage() {
        DummyResource resource = new DummyResource(1L, null);
        when(lmsSecurityService.canManageClass(1L)).thenReturn(true);

        assertTrue(evaluator.hasPermission(authentication, resource, "MANAGE"));
    }

    @Test
    void hasPermission_domainObject_CourseId_Access() {
        DummyResource resource = new DummyResource(null, 10L);
        when(classesRepository.findIdsByCourseId(10L)).thenReturn(List.of(1L, 2L));
        when(lmsSecurityService.canAccessClass(1L)).thenReturn(false);
        when(lmsSecurityService.canAccessClass(2L)).thenReturn(true);

        assertTrue(evaluator.hasPermission(authentication, resource, "READ"));
    }

    @Test
    void hasPermission_domainObject_CourseId_NoClasses() {
        DummyResource resource = new DummyResource(null, 10L);
        when(classesRepository.findIdsByCourseId(10L)).thenReturn(Collections.emptyList());

        assertFalse(evaluator.hasPermission(authentication, resource, "READ"));
    }

    @Test
    void hasPermission_targetIdType_Nulls() {
        assertFalse(evaluator.hasPermission(authentication, null, "Classes", "READ"));
        assertFalse(evaluator.hasPermission(authentication, 1L, null, "READ"));
    }

    @Test
    void hasPermission_targetIdType_ClassNotFound() {
        assertFalse(evaluator.hasPermission(authentication, 1L, "UnknownClass123", "READ"));
    }

    @Test
    void hasPermission_targetIdType_EntityNull() throws Exception {
        Class<?> clazz = Class.forName("com.lms.education.module.academic.entity.Classes");
        doReturn(null).when(entityManager).find((Class) clazz, 1L);

        assertFalse(evaluator.hasPermission(authentication, 1L, "Classes", "READ"));
    }

    @Test
    void hasPermission_targetIdType_NotSecurityResource() throws Exception {
        Class<?> clazz = Class.forName("com.lms.education.module.academic.entity.Classes");
        doReturn(new Object()).when(entityManager).find((Class) clazz, 1L);

        assertFalse(evaluator.hasPermission(authentication, 1L, "Classes", "READ"));
    }

    @Test
    void hasPermission_targetIdType_Success() throws Exception {
        Class<?> clazz = Class.forName("com.lms.education.module.academic.entity.Classes");
        DummyResource resource = new DummyResource(1L, null);
        doReturn(resource).when(entityManager).find((Class) clazz, 1L);
        when(lmsSecurityService.canAccessClass(1L)).thenReturn(true);

        assertTrue(evaluator.hasPermission(authentication, 1L, "Classes", "READ"));
    }

    @Test
    void hasPermission_domainObject_UnsupportedPermission() {
        DummyResource resource = new DummyResource(1L, null);
        assertFalse(evaluator.hasPermission(authentication, resource, "DELETE"));
    }
}
