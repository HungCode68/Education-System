package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.AssignPermissionDto;
import com.lms.education.module.user.dto.RoleDto;
import com.lms.education.module.user.entity.Permission;
import com.lms.education.module.user.entity.Role;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role mockRole;
    private RoleDto mockRoleDto;

    @BeforeEach
    void setUp() {
        mockRole = new Role();
        mockRole.setId(1L);
        mockRole.setName("ROLE_ADMIN");
        mockRole.setDescription("Admin role");

        mockRoleDto = RoleDto.builder()
                .name("admin")
                .description("Admin role")
                .build();
    }

    @Test
    void create_Success_AddsPrefix() {
        when(roleRepository.existsByName("admin")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(mockRole);

        RoleDto result = roleService.create(mockRoleDto);

        assertNotNull(result);
        assertEquals("ROLE_ADMIN", result.getName());
    }

    @Test
    void create_AlreadyHasPrefix_Success() {
        mockRoleDto.setName("ROLE_ADMIN");
        when(roleRepository.existsByName("ROLE_ADMIN")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(mockRole);

        RoleDto result = roleService.create(mockRoleDto);

        assertNotNull(result);
        assertEquals("ROLE_ADMIN", result.getName());
    }

    @Test
    void create_DuplicateRole_ThrowsException() {
        when(roleRepository.existsByName("admin")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> roleService.create(mockRoleDto));
    }

    @Test
    void update_Success() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(mockRole));
        mockRoleDto.setName("ROLE_MANAGER");
        when(roleRepository.existsByName("ROLE_MANAGER")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(mockRole);

        RoleDto result = roleService.update(1L, mockRoleDto);

        assertNotNull(result);
        verify(roleRepository).save(mockRole);
    }

    @Test
    void update_DuplicateRole_ThrowsException() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(mockRole));
        mockRoleDto.setName("ROLE_MANAGER");
        when(roleRepository.existsByName("ROLE_MANAGER")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> roleService.update(1L, mockRoleDto));
    }
    
    @Test
    void update_RoleNotFound_ThrowsException() {
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roleService.update(1L, mockRoleDto));
    }

    @Test
    void delete_Success() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(mockRole));
        roleService.delete(1L);
        verify(roleRepository).delete(mockRole);
    }

    @Test
    void delete_RoleNotFound_ThrowsException() {
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roleService.delete(1L));
    }

    @Test
    void getById_Success() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(mockRole));
        RoleDto result = roleService.getById(1L);
        assertEquals("ROLE_ADMIN", result.getName());
    }

    @Test
    void getById_RoleNotFound_ThrowsException() {
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> roleService.getById(1L));
    }

    @Test
    void getByName_Success() {
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(mockRole));
        RoleDto result = roleService.getByName("ROLE_ADMIN");
        assertEquals(1L, result.getId());
    }

    @Test
    void getByName_RoleNotFound_ThrowsException() {
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> roleService.getByName("ROLE_ADMIN"));
    }

    @Test
    void getAllRoles_WithKeyword() {
        Page<Role> page = new PageImpl<>(List.of(mockRole));
        Pageable pageable = PageRequest.of(0, 10);
        when(roleRepository.findByNameContainingIgnoreCase("ADMIN", pageable)).thenReturn(page);

        Page<RoleDto> result = roleService.getAllRoles("ADMIN", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllRoles_WithoutKeyword() {
        Page<Role> page = new PageImpl<>(List.of(mockRole));
        Pageable pageable = PageRequest.of(0, 10);
        when(roleRepository.findAll(pageable)).thenReturn(page);

        Page<RoleDto> result = roleService.getAllRoles(null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void assignPermissions_Success() {
        AssignPermissionDto dto = new AssignPermissionDto();
        dto.setRoleId(1L);
        dto.setPermissionIds(Set.of(1L, 2L));

        when(roleRepository.findById(1L)).thenReturn(Optional.of(mockRole));
        
        Permission p1 = new Permission(); p1.setId(1L);
        Permission p2 = new Permission(); p2.setId(2L);
        when(permissionRepository.findAllById(dto.getPermissionIds())).thenReturn(List.of(p1, p2));
        
        when(roleRepository.save(any(Role.class))).thenReturn(mockRole);

        RoleDto result = roleService.assignPermissions(dto);

        assertNotNull(result);
        assertEquals(2, mockRole.getPermissions().size());
        verify(roleRepository).save(mockRole);
    }
    
    @Test
    void assignPermissions_RoleNotFound_ThrowsException() {
        AssignPermissionDto dto = new AssignPermissionDto();
        dto.setRoleId(1L);
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roleService.assignPermissions(dto));
    }
}
