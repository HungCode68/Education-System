package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.AssignPermissionDto;
import com.lms.education.module.user.dto.PermissionDto;
import com.lms.education.module.user.dto.RoleDto;
import com.lms.education.module.user.entity.Permission;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.repository.PermissionRepository;
import com.lms.education.module.user.repository.RoleRepository;
import com.lms.education.module.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public RoleDto create(RoleDto dto) {

        // Tự động viết hoa và thêm tiền tố ROLE_ nếu người dùng chưa nhập
        String formattedRoleName = dto.getName().trim().toUpperCase();
        if (!formattedRoleName.startsWith("ROLE_")) {
            formattedRoleName = "ROLE_" + formattedRoleName;
        }

        if (roleRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Tên vai trò '" + dto.getName() + "' đã tồn tại!");
        }

        Role role = Role.builder()
                .name(formattedRoleName)
                .description(dto.getDescription())
                .build();

        Role savedRole = roleRepository.save(role);
        log.info("Đã tạo vai trò mới: {}", savedRole.getName());
        return mapToDto(savedRole);
    }

    @Override
    @Transactional
    public RoleDto update(Long id, RoleDto dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với ID: " + id));

        // Kiểm tra nếu đổi tên trùng với role khác
        if (!role.getName().equals(dto.getName()) && roleRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Tên vai trò '" + dto.getName() + "' đã tồn tại!");
        }

        role.setName(dto.getName());
        role.setDescription(dto.getDescription());

        return mapToDto(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với ID: " + id));

        // Thực hiện xóa
        roleRepository.delete(role);
        log.info("Đã xóa hoàn toàn vai trò: {}", role.getName());
    }

    @Override
    public RoleDto getById(Long id) {
        return roleRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với ID: " + id));
    }

    @Override
    public RoleDto getByName(String name) {
        return roleRepository.findByName(name)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò: " + name));
    }

    @Override
    public Page<RoleDto> getAllRoles(String keyword, Pageable pageable) {
        Page<Role> roles;
        if (keyword != null && !keyword.isEmpty()) {
            roles = roleRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            roles = roleRepository.findAll(pageable);
        }
        return roles.map(this::mapToDto);
    }

    @Override
    @Transactional
    public RoleDto assignPermissions(AssignPermissionDto dto) {
        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò!"));

        List<Permission> permissions = permissionRepository.findAllById(dto.getPermissionIds());
        role.setPermissions(new HashSet<>(permissions));

        return mapToDto(roleRepository.save(role));
    }

    // --- Helper Method ---
    private RoleDto mapToDto(Role role) {
        Set<PermissionDto> permissionDtos = null;
        if (role.getPermissions() != null) {
            permissionDtos = role.getPermissions().stream()
                    .map(p -> PermissionDto.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .description(p.getDescription())
                            .build())
                    .collect(Collectors.toSet());
        }

        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .createdAt(role.getCreatedAt())
                .permissions(permissionDtos)
                .build();
    }
}