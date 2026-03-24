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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public PermissionDto create(PermissionDto dto) {
        String codeUpperCase = dto.getCode().toUpperCase();

        // Kiểm tra trùng lặp mã quyền
        if (permissionRepository.existsByCode(codeUpperCase)) {
            throw new DuplicateResourceException("Mã quyền (Code) '" + codeUpperCase + "' đã tồn tại trong hệ thống!");
        }

        Permission permission = Permission.builder()
                .code(codeUpperCase)
                .scope(dto.getScope())
                .name(dto.getName())
                .description(dto.getDescription())
                .build();

        Permission savedPermission = permissionRepository.save(permission);
        log.info("Đã tạo mới thành công Quyền: {} ({})", savedPermission.getName(), savedPermission.getCode());

        return mapToDto(savedPermission);
    }

    @Override
    @Transactional
    public PermissionDto update(Integer id, PermissionDto dto) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền với ID: " + id));

        String newCode = dto.getCode().toUpperCase();

        // Nếu thay đổi mã quyền, phải kiểm tra xem mã mới có bị trùng với quyền khác không
        if (!permission.getCode().equals(newCode) && permissionRepository.existsByCode(newCode)) {
            throw new DuplicateResourceException("Mã quyền (Code) '" + newCode + "' đã được sử dụng!");
        }

        permission.setCode(newCode);
        permission.setScope(dto.getScope());
        permission.setName(dto.getName());
        permission.setDescription(dto.getDescription());

        Permission updatedPermission = permissionRepository.save(permission);
        log.info("Đã cập nhật Quyền ID: {}", id);

        return mapToDto(updatedPermission);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền với ID: " + id));

        // Kiểm tra xem quyền này đã được gán cho Role nào chưa
        boolean isAssigned = roleRepository.isPermissionAssigned(id);

        if (isAssigned) {
            // NẾU ĐÃ GÁN -> CHẶN KHÔNG CHO XÓA VÀ BÁO LỖI
            log.warn("Cố gắng xóa quyền ID: {} nhưng quyền này đang được gán cho Role", id);

            // Bạn có thể dùng RuntimeException hoặc OperationNotPermittedException (nếu project đã có)
            throw new RuntimeException("Không thể xóa! Quyền này đang được gán cho một hoặc nhiều vai trò. Vui lòng gỡ quyền khỏi các vai trò trước khi xóa.");
        } else {
            // NẾU CHƯA GÁN -> XÓA CỨNG (Xóa sạch khỏi DB)
            permissionRepository.delete(permission);
            log.info("Quyền ID: {} chưa được gán cho Role nào. Đã thực hiện XÓA CỨNG thành công", id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionDto getById(Integer id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền với ID: " + id));
        return mapToDto(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getByScope(Permission.PermissionScope scope) {
        return permissionRepository.findByScope(scope).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PermissionDto> getAllPermissions(String keyword, Permission.PermissionScope scope, Pageable pageable) {
        Page<Permission> permissions;

        if (keyword != null && !keyword.trim().isEmpty()) {
            permissions = permissionRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(keyword, keyword, pageable);
        } else if (scope != null) {
            permissions = permissionRepository.findByScope(scope, pageable);
        } else {
            permissions = permissionRepository.findAll(pageable);
        }

        return permissions.map(this::mapToDto);
    }

    // --- Hàm Helper ---
    private PermissionDto mapToDto(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .scope(permission.getScope())
                .name(permission.getName())
                .description(permission.getDescription())
                .build();
    }
}
