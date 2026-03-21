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
        //  Kiểm tra mã Code đã tồn tại chưa
        if (roleRepository.existsByCode(dto.getCode().toUpperCase())) {
            throw new DuplicateResourceException("Mã vai trò (Code) '" + dto.getCode() + "' đã tồn tại trong hệ thống!");
        }

        //  Build Entity
        Role role = Role.builder()
                .code(dto.getCode().toUpperCase()) // Luôn lưu code dưới dạng IN HOA cho đồng bộ
                .name(dto.getName())
                .status(dto.getStatus() != null ? dto.getStatus() : Role.RoleStatus.active)
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();

        // Lưu và trả về
        Role savedRole = roleRepository.save(role);
        log.info("Đã tạo mới thành công Vai trò: {} ({})", savedRole.getName(), savedRole.getCode());

        return mapToDto(savedRole);
    }

    @Override
    @Transactional
    public RoleDto update(String id, RoleDto dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với ID: " + id));

        // Kiểm tra nếu người dùng muốn đổi mã Code -> Phải chắc chắn mã mới không bị trùng với Role khác
        String newCode = dto.getCode().toUpperCase();
        if (!role.getCode().equals(newCode) && roleRepository.existsByCode(newCode)) {
            throw new DuplicateResourceException("Mã vai trò (Code) '" + newCode + "' đã được sử dụng bởi một vai trò khác!");
        }

        role.setCode(newCode);
        role.setName(dto.getName());
        if (dto.getStatus() != null) {
            role.setStatus(dto.getStatus());
        }

        Role updatedRole = roleRepository.save(role);
        log.info("Đã cập nhật Vai trò ID: {}", id);

        return mapToDto(updatedRole);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với ID: " + id));

        // Lưu ý: Trong thực tế, nếu Role đã được gán cho User, việc xóa cứng (delete) có thể gây lỗi Foreign Key.
        // Giải pháp an toàn là chuyển trạng thái sang Inactive (Xóa mềm):
         role.setStatus(Role.RoleStatus.inactive);
         roleRepository.save(role);
        log.info("Đã xóa Vai trò ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto getById(String id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với ID: " + id));
        return mapToDto(role);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto getByCode(String code) {
        Role role = roleRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với mã: " + code));
        return mapToDto(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleDto> getAllRoles(String keyword, Role.RoleStatus status, Pageable pageable) {
        Page<Role> roles;

        // Xử lý logic tìm kiếm và lọc
        if (keyword != null && !keyword.trim().isEmpty()) {
            roles = roleRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(keyword, keyword, pageable);
        } else if (status != null) {
            roles = roleRepository.findByStatus(status, pageable);
        } else {
            roles = roleRepository.findAll(pageable);
        }

        return roles.map(this::mapToDto);
    }

    @Override
    @Transactional
    public RoleDto assignPermissions(AssignPermissionDto dto) {
        // Tìm Role cần cấp quyền
        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với ID: " + dto.getRoleId()));

        // Lấy danh sách các Permission tương ứng với mảng ID mà Frontend gửi lên
        List<Permission> permissions = permissionRepository.findAllById(dto.getPermissionIds());

        if (permissions.isEmpty() && !dto.getPermissionIds().isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy các quyền hợp lệ để gán!");
        }

        // Gán danh sách quyền vào cho Role
        // Hibernate sẽ tự động so sánh: Quyền nào mới thì INSERT, Quyền nào bị bỏ tích thì DELETE trong bảng role_permission
        role.setPermissions(new HashSet<>(permissions));

        // Lưu lại
        Role updatedRole = roleRepository.save(role);
        log.info("Đã cập nhật phân quyền thành công cho Vai trò: {}", updatedRole.getCode());

        return mapToDto(updatedRole);
    }

    // --- Hàm Helper ---
    private RoleDto mapToDto(Role role) {
        Set<PermissionDto> permissionDtos = null;
        if (role.getPermissions() != null) {
            permissionDtos = role.getPermissions().stream()
                    .map(p -> PermissionDto.builder()
                            .id(p.getId())
                            .code(p.getCode())
                            .scope(p.getScope())
                            .name(p.getName())
                            .description(p.getDescription())
                            .build())
                    .collect(Collectors.toSet());
        }

        return RoleDto.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .status(role.getStatus())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .permissions(permissionDtos)
                .build();
    }
}
