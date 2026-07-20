package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.PermissionDto;
import com.lms.education.module.user.entity.Permission;
import com.lms.education.module.user.repository.PermissionRepository;
import com.lms.education.module.user.repository.RoleRepository;
import com.lms.education.module.user.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public PermissionDto create(PermissionDto dto) {
        String nameUpperCase = dto.getName().toUpperCase(); // Ép tên quyền viết hoa (VD: COURSE_CREATE)

        if (permissionRepository.existsByName(nameUpperCase)) {
            throw new DuplicateResourceException("Tên quyền '" + nameUpperCase + "' đã tồn tại trong hệ thống!");
        }

        Permission permission = Permission.builder()
                .name(nameUpperCase)
                .description(dto.getDescription())
                .build();

        Permission savedPermission = permissionRepository.save(permission);
        log.info("Đã tạo mới quyền: {}", savedPermission.getName());

        return mapToDto(savedPermission);
    }

    @Override
    @Transactional
    public PermissionDto update(Long id, PermissionDto dto) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền với ID: " + id));

        String newName = dto.getName().toUpperCase();

        if (!permission.getName().equals(newName) && permissionRepository.existsByName(newName)) {
            throw new DuplicateResourceException("Tên quyền '" + newName + "' đã được sử dụng!");
        }

        permission.setName(newName);
        permission.setDescription(dto.getDescription());

        Permission updatedPermission = permissionRepository.save(permission);
        return mapToDto(updatedPermission);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền với ID: " + id));

        // Kiểm tra xem quyền này đã được gán cho Role nào chưa
        boolean isAssigned = roleRepository.isPermissionAssigned(id);

        if (isAssigned) {
            throw new RuntimeException("Không thể xóa! Quyền này đang được gán cho một hoặc nhiều vai trò.");
        } else {
            permissionRepository.delete(permission);
            log.info("Đã xóa hoàn toàn quyền ID: {}", id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionDto getById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền với ID: " + id));
        return mapToDto(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PermissionDto> getAllPermissions(String keyword, Pageable pageable) {
        Page<Permission> permissions;

        if (keyword != null && !keyword.trim().isEmpty()) {
            permissions = permissionRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            permissions = permissionRepository.findAll(pageable);
        }

        return permissions.map(this::mapToDto);
    }

    // --- Hàm Helper ---
    private PermissionDto mapToDto(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .build();
    }
}