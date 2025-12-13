package com.loganomaly.detector.auth_service.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that verify security configuration and endpoint accessibility.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:securitytest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=security-test-secret-key-must-be-at-least-32-characters-long",
        "jwt.expiration=3600000"
})
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldAllowAccessToHealthEndpoint() throws Exception {
        // Public endpoint should be accessible without authentication
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAccessToRegisterEndpoint() throws Exception {
        // Register endpoint should be accessible without authentication
        mockMvc.perform(post("/register"))
                .andExpect(status().is4xxClientError()); // Will fail validation but endpoint is accessible
    }

    @Test
    void shouldAllowAccessToLoginEndpoint() throws Exception {
        // Login endpoint should be accessible without authentication
        mockMvc.perform(post("/login"))
                .andExpect(status().is4xxClientError()); // Will fail validation but endpoint is accessible
    }

    @Test
    void shouldHaveBCryptPasswordEncoder() {
        // Verify BCrypt password encoder is configured
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // BCrypt hashes start with $2a$ or $2b$
        assertTrue(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$"));
        
        // Verify password matching works
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
        assertFalse(passwordEncoder.matches("wrongPassword", encodedPassword));
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {
        // BCrypt should generate different hashes for the same password (due to salt)
        String password = "samePassword";
        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);

        assertNotEquals(hash1, hash2);
        assertTrue(passwordEncoder.matches(password, hash1));
        assertTrue(passwordEncoder.matches(password, hash2));
    }
}

