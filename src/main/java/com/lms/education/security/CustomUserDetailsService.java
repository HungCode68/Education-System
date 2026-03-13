package com.lms.education.security;

import com.lms.education.module.user.entity.Permission;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với email: " + email));

        List<GrantedAuthority> authorities = new ArrayList<>();

        if (user.getRole() != null) {
            // Thêm Role (Gắn cờ ROLE_ tạm thời để AuthController nhận diện)
            // Ví dụ DB là "STUDENT" -> Trong RAM sẽ là "ROLE_STUDENT"
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getCode()));

            // Thêm toàn bộ mã Permission
            // Ví dụ: "USER_CREATE", "CLASS_VIEW"
            if (user.getRole().getPermissions() != null) {
                for (Permission permission : user.getRole().getPermissions()) {
                    authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                }
            }
        }

        // Đẩy dữ liệu sang UserPrincipal
        return UserPrincipal.build(user, authorities);
    }
}
