package com.lms.education.security;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
public class LmsPermissionEvaluator implements PermissionEvaluator {

    private final LmsSecurityService lmsSecurityService;
    private final EntityManager entityManager;
    private final com.lms.education.module.academic.repository.ClassesRepository classesRepository;

    private static final String ENTITY_PACKAGE_LMS = "com.lms.education.module.lms.entity.";
    private static final String ENTITY_PACKAGE_ACADEMIC = "com.lms.education.module.academic.entity.";
    private static final String ENTITY_PACKAGE_NOTIFICATION = "com.lms.education.module.notification.entity.";

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (targetDomainObject == null) return false;
        
        if (targetDomainObject instanceof SecurityResource resource) {
            return evaluateResource(resource, (String) permission);
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (targetId == null || targetType == null) return false;

        Class<?> clazz = resolveClass(targetType);
        if (clazz == null) return false;

        Object entity = entityManager.find(clazz, targetId);
        if (entity instanceof SecurityResource resource) {
            return evaluateResource(resource, (String) permission);
        }
        return false;
    }

    private Class<?> resolveClass(String targetType) {
        // Try common packages
        String[] packages = { ENTITY_PACKAGE_LMS, ENTITY_PACKAGE_ACADEMIC, ENTITY_PACKAGE_NOTIFICATION };
        for (String pkg : packages) {
            try {
                return Class.forName(pkg + targetType);
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    private boolean evaluateResource(SecurityResource resource, String permission) {
        Long classId = resource.extractClassId();
        if (classId != null && evaluateClassPermission(classId, permission)) {
            return true;
        }

        Long courseId = resource.extractCourseId();
        if (courseId != null && evaluateCoursePermission(courseId, permission)) {
            return true;
        }

        return false;
    }

    private boolean evaluateCoursePermission(Long courseId, String permission) {
        // Resolve course → classes rồi tái sử dụng evaluateClassPermission
        // Không cần thêm hàm mới vào LmsSecurityService
        java.util.List<Long> classIds = classesRepository.findIdsByCourseId(courseId);
        return classIds.stream().anyMatch(classId -> evaluateClassPermission(classId, permission));
    }

    private boolean evaluateClassPermission(Long classId, String permission) {
        if ("READ".equalsIgnoreCase(permission) || "ACCESS".equalsIgnoreCase(permission)) {
            return lmsSecurityService.canAccessClass(classId);
        } else if ("MANAGE".equalsIgnoreCase(permission)) {
            return lmsSecurityService.canManageClass(classId);
        }
        return false;
    }
}
