package com.lms.education.integration;

import com.lms.education.module.user.dto.LoginRequest;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        Optional<User> existing = userRepository.findByEmail("testauth@example.com");
        if (existing.isEmpty()) {
            User user = new User();
            user.setEmail("testauth@example.com");
            user.setPassword(passwordEncoder.encode("Password123!"));
            user.setFullName("Test Auth User");
            user.setStatus("ACTIVE");
            testUser = userRepository.save(user);
        } else {
            testUser = existing.get();
        }
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("testauth@example.com");
        request.setPassword("Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.fullName").value("Test Auth User"))
                .andExpect(cookie().exists("lms_refresh_token"));
    }

    @Test
    void testLogin_WrongPassword_ReturnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("testauth@example.com");
        request.setPassword("WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testLogin_InactiveUser_ReturnsForbidden() throws Exception {
        // Change user status to INACTIVE
        testUser.setStatus("INACTIVE");
        userRepository.save(testUser);

        LoginRequest request = new LoginRequest();
        request.setEmail("testauth@example.com");
        request.setPassword("Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());
    }
}
