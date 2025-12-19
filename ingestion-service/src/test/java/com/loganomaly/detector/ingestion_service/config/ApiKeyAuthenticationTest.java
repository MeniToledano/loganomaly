package com.loganomaly.detector.ingestion_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loganomaly.detector.common.dto.LogEventRequest;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.topic.log-events=log-events",
        "ingestion.api-key=test-api-key-12345"
})
class ApiKeyAuthenticationTest {

    private static final String VALID_API_KEY = "test-api-key-12345";
    private static final String INVALID_API_KEY = "wrong-api-key";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Setup KafkaTemplate mock to return a completed future
        @SuppressWarnings("unchecked")
        SendResult<String, String> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("log-events", 0), 0, 0, 0, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
    }

    @Test
    void shouldAllowAccessWithValidApiKey() throws Exception {
        LogEventRequest request = LogEventRequest.builder()
                .level("INFO")
                .message("Test log")
                .service("test-service")
                .build();

        mockMvc.perform(post("/api/logs")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    void shouldDenyAccessWithInvalidApiKey() throws Exception {
        LogEventRequest request = LogEventRequest.builder()
                .level("INFO")
                .message("Test log")
                .service("test-service")
                .build();

        mockMvc.perform(post("/api/logs")
                        .header("X-API-Key", INVALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyAccessWithoutApiKey() throws Exception {
        LogEventRequest request = LogEventRequest.builder()
                .level("INFO")
                .message("Test log")
                .service("test-service")
                .build();

        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowHealthEndpointWithoutApiKey() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("ingestion-service"));
    }

    @Test
    void shouldDenyBatchEndpointWithoutApiKey() throws Exception {
        mockMvc.perform(post("/api/logs/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logs\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowBatchEndpointWithValidApiKey() throws Exception {
        mockMvc.perform(post("/api/logs/batch")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logs\":[]}"))
                .andExpect(status().isBadRequest()); // Empty batch returns 400, but auth passed
    }

    @Test
    void shouldBeCaseSensitiveForApiKey() throws Exception {
        LogEventRequest request = LogEventRequest.builder()
                .level("INFO")
                .message("Test log")
                .service("test-service")
                .build();

        // Test with different case - API keys are case sensitive
        mockMvc.perform(post("/api/logs")
                        .header("X-API-Key", "TEST-API-KEY-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

