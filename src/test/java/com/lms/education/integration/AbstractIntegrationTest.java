package com.lms.education.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.education.service.MinioStorageService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Base class for all integration tests.
 * This class configures the Spring context, MockMvc, and external service mocks.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional // Automatically rollback database changes after each test
@Tag("integration")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // Mock external services to avoid actual connections during tests
    @MockBean
    protected MinioStorageService minioStorageService;

    @MockBean
    protected ChatClient chatClient;
    
    // We can add helper methods here, for example, to get a JWT token
}
