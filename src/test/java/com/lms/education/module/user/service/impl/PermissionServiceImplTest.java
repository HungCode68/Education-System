package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.PermissionDto;
import com.lms.education.module.user.entity.Permission;
import com.lms.education.module.user.repository.PermissionRepository;
import com.lms.education.module.user.repository.RoleRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    private Permission mockPermission;
    private PermissionDto mockPermissionDto;

    @BeforeEach
    void setUp() {
        mockPermission = new Permission();
        mockPermission.setId(1L);
        mockPermission.setName("USER_CREATE");
        mockPermission.setDescription("Create user permission");

        mockPermissionDto = PermissionDto.builder()
                .name("user_create")
                .description("Create user permission")
                .build();
    }

    @Test
    void create_Success_ConvertsToUpperCase() {
        when(permissionRepository.existsByName("USER_CREATE")).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenReturn(mockPermission);

        PermissionDto result = permissionService.create(mockPermissionDto);

        assertNotNull(result);
        assertEquals("USER_CREATE", result.getName());
    }

    @Test
    void create_DuplicatePermission_ThrowsException() {
        when(permissionRepository.existsByName("USER_CREATE")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> permissionService.create(mockPermissionDto));
    }

    @Test
    void update_Success() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(mockPermission));
        mockPermissionDto.setName("USER_UPDATE");
        when(permissionRepository.existsByName("USER_UPDATE")).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenReturn(mockPermission);

        PermissionDto result = permissionService.update(1L, mockPermissionDto);

        assertNotNull(result);
        verify(permissionRepository).save(mockPermission);
    }

    @Test
    void update_DuplicatePermission_ThrowsException() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(mockPermission));
        mockPermissionDto.setName("USER_UPDATE");
        when(permissionRepository.existsByName("USER_UPDATE")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> permissionService.update(1L, mockPermissionDto));
    }

    @Test
    void update_PermissionNotFound_ThrowsException() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> permissionService.update(1L, mockPermissionDto));
    }

    @Test
    void delete_Success() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(mockPermission));
        when(roleRepository.isPermissionAssigned(1L)).thenReturn(false);

        permissionService.delete(1L);

        verify(permissionRepository).delete(mockPermission);
    }

    @Test
    void delete_PermissionAssigned_ThrowsException() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(mockPermission));
        when(roleRepository.isPermissionAssigned(1L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> permissionService.delete(1L));
    }

    @Test
    void delete_PermissionNotFound_ThrowsException() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> permissionService.delete(1L));
    }

    @Test
    void getById_Success() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(mockPermission));
        PermissionDto result = permissionService.getById(1L);
        assertEquals("USER_CREATE", result.getName());
    }

    @Test
    void getById_PermissionNotFound_ThrowsException() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> permissionService.getById(1L));
    }

    @Test
    void getAllPermissions_WithKeyword() {
        Page<Permission> page = new PageImpl<>(List.of(mockPermission));
        Pageable pageable = PageRequest.of(0, 10);
        when(permissionRepository.findByNameContainingIgnoreCase("USER", pageable)).thenReturn(page);

        Page<PermissionDto> result = permissionService.getAllPermissions("USER", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllPermissions_WithoutKeyword() {
        Page<Permission> page = new PageImpl<>(List.of(mockPermission));
        Pageable pageable = PageRequest.of(0, 10);
        when(permissionRepository.findAll(pageable)).thenReturn(page);

        Page<PermissionDto> result = permissionService.getAllPermissions(null, pageable);

        assertEquals(1, result.getTotalElements());
    }
}
