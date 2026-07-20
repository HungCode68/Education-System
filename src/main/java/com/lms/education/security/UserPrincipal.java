package com.lms.education.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lms.education.module.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@Builder
public class UserPrincipal implements UserDetails {

    private Long id;

    private String email;

    @JsonIgnore
    private String password;

    private String fullName;

    private String status;

    private List<String> roleDescriptions;

    private List<String> permissionList;

    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        Set<GrantedAuthority> authoritySet = new HashSet<>();
        List<String> roleDescs = new ArrayList<>();

        Set<String> exactPermissions = new HashSet<>();

        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                // Thêm Role cho Spring Security
                authoritySet.add(new SimpleGrantedAuthority(role.getName()));

                // Lấy mô tả Role cho Frontend
                if (role.getDescription() != null && !role.getDescription().trim().isEmpty()) {
                    roleDescs.add(role.getDescription());
                } else {
                    roleDescs.add(role.getName().replace("ROLE_", ""));
                }

                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(permission -> {
                        // Thêm Permission cho Spring Security
                        authoritySet.add(new SimpleGrantedAuthority(permission.getName()));

                        // LƯU CHÍNH XÁC TÊN PERMISSION VÀO SET
                        exactPermissions.add(permission.getName());
                    });
                }
            });
        }

        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .roleDescriptions(roleDescs)
                .permissionList(new ArrayList<>(exactPermissions))
                .authorities(new ArrayList<>(authoritySet))
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Tài khoản không bị khóa nếu status không phải là 'LOCKED'
        return !"LOCKED".equalsIgnoreCase(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Tài khoản chỉ hoạt động nếu status là 'ACTIVE'
        return "ACTIVE".equalsIgnoreCase(status);
    }
}