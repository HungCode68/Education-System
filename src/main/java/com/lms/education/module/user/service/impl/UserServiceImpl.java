package com.lms.education.module.user.service.impl;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.RoleRepository;
import com.lms.education.module.user.repository.UserRepository;
import com.lms.education.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void changeUserRole(String userId, String roleCode) {
        // Tìm User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản (User ID: " + userId + ")"));

        // Tìm Role mới trong Database
        Role newRole = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền có mã: " + roleCode));

        // Cập nhật Role cho User
        user.setRole(newRole);
        userRepository.save(user);

        log.info("Đã thay đổi quyền của User {} thành {}", user.getEmail(), roleCode);
    }
}
