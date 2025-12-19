package com.loganomaly.detector.ingestion_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loganomaly.detector.common.dto.BatchLogRequest;
import com.loganomaly.detector.common.dto.BatchLogResponse;
import com.loganomaly.detector.common.dto.LogEventRequest;
import com.loganomaly.detector.common.dto.LogEventResponse;
import com.loganomaly.detector.ingestion_service.service.LogIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LogController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
        })
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockBean
    private LogIngestionService logIngestionService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldIngestSingleLog() throws Exception {
        // Given
        LogEventRequest request = LogEventRequest.builder()
                .timestamp(Instant.parse("2025-12-19T10:00:00Z"))
                .level("INFO")
                .message("Test log message")
                .service("test-service")
                .metadata(Map.of("key", "value"))
                .build();

        LogEventResponse response = LogEventResponse.builder()
                .id("test-uuid-123")
                .status("ACCEPTED")
                .timestamp(Instant.now())
                .build();

        when(logIngestionService.ingestLog(any(LogEventRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("test-uuid-123"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void shouldReturnErrorForFailedIngestion() throws Exception {
        // Given
        LogEventRequest request = LogEventRequest.builder()
                .level("ERROR")
                .message("Test error log")
                .service("test-service")
                .build();

        LogEventResponse response = LogEventResponse.builder()
                .id("test-uuid-456")
                .status("FAILED")
                .timestamp(Instant.now())
                .build();

        when(logIngestionService.ingestLog(any(LogEventRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void shouldIngestBatchLogs() throws Exception {
        // Given
        List<LogEventRequest> logs = Arrays.asList(
                LogEventRequest.builder()
                        .level("INFO")
                        .message("Log 1")
                        .service("service-a")
                        .build(),
                LogEventRequest.builder()
                        .level("ERROR")
                        .message("Log 2")
                        .service("service-b")
                        .build(),
                LogEventRequest.builder()
                        .level("WARN")
                        .message("Log 3")
                        .service("service-a")
                        .build()
        );

        BatchLogRequest request = BatchLogRequest.builder()
                .logs(logs)
                .build();

        BatchLogResponse response = BatchLogResponse.builder()
                .acceptedCount(3)
                .failedCount(0)
                .acceptedIds(List.of("id-1", "id-2", "id-3"))
                .timestamp(Instant.now())
                .build();

        when(logIngestionService.ingestBatch(any(BatchLogRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/logs/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.acceptedCount").value(3))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.acceptedIds.length()").value(3));
    }

    @Test
    void shouldReturnBadRequestForEmptyBatch() throws Exception {
        // Given
        BatchLogRequest request = BatchLogRequest.builder()
                .logs(List.of())
                .build();

        // When & Then
        mockMvc.perform(post("/api/logs/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.acceptedCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(0));
    }

    @Test
    void shouldReturnBadRequestForNullBatch() throws Exception {
        // Given
        BatchLogRequest request = new BatchLogRequest();

        // When & Then
        mockMvc.perform(post("/api/logs/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandlePartialBatchFailure() throws Exception {
        // Given
        List<LogEventRequest> logs = Arrays.asList(
                LogEventRequest.builder()
                        .level("INFO")
                        .message("Log 1")
                        .service("service-a")
                        .build(),
                LogEventRequest.builder()
                        .level("ERROR")
                        .message("Log 2")
                        .service("service-b")
                        .build()
        );

        BatchLogRequest request = BatchLogRequest.builder()
                .logs(logs)
                .build();

        BatchLogResponse response = BatchLogResponse.builder()
                .acceptedCount(1)
                .failedCount(1)
                .acceptedIds(List.of("id-1"))
                .timestamp(Instant.now())
                .build();

        when(logIngestionService.ingestBatch(any(BatchLogRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/logs/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.acceptedCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(1));
    }
}

