package com.lms.education.integration;

import com.lms.education.module.user.dto.ChangePasswordRequest;
import com.lms.education.module.user.dto.UserDto;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.UserRepository;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Optional;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByName("ROLE_STUDENT").isEmpty()) {
            Role role = new Role();
            role.setName("ROLE_STUDENT");
            role.setDescription("Student Role");
            roleRepository.save(role);
        }

        Optional<User> existing = userRepository.findByEmail("testuser@example.com");
        if (existing.isEmpty()) {
            User user = new User();
            user.setEmail("testuser@example.com");
            user.setPassword(passwordEncoder.encode("Password123!"));
            user.setFullName("Test User");
            user.setStatus("ACTIVE");
            testUser = userRepository.save(user);
        } else {
            testUser = existing.get();
        }
    }

    @Test
    @WithMockUser(authorities = {"ACCOUNT_VIEW"})
    void testGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(authorities = {"ACCOUNT_CREATE"})
    void testCreateUser() throws Exception {
        UserDto newUser = new UserDto();
        newUser.setEmail("newuser@example.com");
        newUser.setFullName("New User");
        newUser.setStatus("ACTIVE");
        newUser.setRoles(Set.of("ROLE_STUDENT"));

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"));
    }

    @Test
    @WithMockUser(authorities = {"ACCOUNT_UPDATE"})
    void testUpdateUserStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/users/" + testUser.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
                
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assert "INACTIVE".equals(updatedUser.getStatus());
    }

    @Test
    @WithMockUser(username = "testuser@example.com")
    void testChangePassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("Password123!");
        request.setNewPassword("NewPassword123!");

        mockMvc.perform(patch("/api/v1/users/me/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công!"));
                
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assert passwordEncoder.matches("NewPassword123!", updatedUser.getPassword());
    }
}
