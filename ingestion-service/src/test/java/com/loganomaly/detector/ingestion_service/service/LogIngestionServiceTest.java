package com.loganomaly.detector.ingestion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loganomaly.detector.common.dto.BatchLogRequest;
import com.loganomaly.detector.common.dto.BatchLogResponse;
import com.loganomaly.detector.common.dto.LogEventRequest;
import com.loganomaly.detector.common.dto.LogEventResponse;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogIngestionServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;

    private LogIngestionService logIngestionService;

    private static final String TEST_TOPIC = "log-events";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        logIngestionService = new LogIngestionService(kafkaTemplate, objectMapper);
        ReflectionTestUtils.setField(logIngestionService, "logEventsTopic", TEST_TOPIC);
    }

    @Test
    void shouldIngestLogSuccessfully() {
        // Given
        LogEventRequest request = LogEventRequest.builder()
                .timestamp(Instant.parse("2025-12-19T10:00:00Z"))
                .level("INFO")
                .message("Test message")
                .service("test-service")
                .metadata(Map.of("key", "value"))
                .build();

        SendResult<String, String> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(TEST_TOPIC, 0), 0, 0, 0, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(TEST_TOPIC), anyString(), anyString())).thenReturn(future);

        // When
        LogEventResponse response = logIngestionService.ingestLog(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getTimestamp()).isNotNull();
        
        verify(kafkaTemplate).send(eq(TEST_TOPIC), anyString(), anyString());
    }

    @Test
    void shouldSetTimestampIfNotProvided() {
        // Given
        LogEventRequest request = LogEventRequest.builder()
                .level("INFO")
                .message("Test message without timestamp")
                .service("test-service")
                .build();

        SendResult<String, String> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(TEST_TOPIC, 0), 0, 0, 0, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(TEST_TOPIC), anyString(), anyString())).thenReturn(future);

        // When
        LogEventResponse response = logIngestionService.ingestLog(request);

        // Then
        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        assertThat(request.getTimestamp()).isNotNull(); // Should have been set
    }

    @Test
    void shouldGenerateUniqueIdsForEachLog() {
        // Given
        LogEventRequest request1 = LogEventRequest.builder()
                .level("INFO")
                .message("Message 1")
                .service("test-service")
                .build();

        LogEventRequest request2 = LogEventRequest.builder()
                .level("INFO")
                .message("Message 2")
                .service("test-service")
                .build();

        SendResult<String, String> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(TEST_TOPIC, 0), 0, 0, 0, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(TEST_TOPIC), anyString(), anyString())).thenReturn(future);

        // When
        LogEventResponse response1 = logIngestionService.ingestLog(request1);
        LogEventResponse response2 = logIngestionService.ingestLog(request2);

        // Then
        assertThat(response1.getId()).isNotEqualTo(response2.getId());
    }

    @Test
    void shouldIngestBatchSuccessfully() {
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

        SendResult<String, String> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(TEST_TOPIC, 0), 0, 0, 0, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(TEST_TOPIC), anyString(), anyString())).thenReturn(future);

        // When
        BatchLogResponse response = logIngestionService.ingestBatch(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAcceptedCount()).isEqualTo(3);
        assertThat(response.getFailedCount()).isEqualTo(0);
        assertThat(response.getAcceptedIds()).hasSize(3);
        assertThat(response.getTimestamp()).isNotNull();
        
        verify(kafkaTemplate, times(3)).send(eq(TEST_TOPIC), anyString(), anyString());
    }

    @Test
    void shouldHandleDifferentLogLevels() {
        // Given
        String[] levels = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL"};
        
        SendResult<String, String> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(TEST_TOPIC, 0), 0, 0, 0, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(TEST_TOPIC), anyString(), anyString())).thenReturn(future);

        // When & Then
        for (String level : levels) {
            LogEventRequest request = LogEventRequest.builder()
                    .level(level)
                    .message("Test message for " + level)
                    .service("test-service")
                    .build();

            LogEventResponse response = logIngestionService.ingestLog(request);
            assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        }
    }

    @Test
    void shouldIncludeMetadataInKafkaMessage() {
        // Given
        Map<String, String> metadata = Map.of(
                "user_id", "123",
                "request_id", "abc-456",
                "environment", "production"
        );

        LogEventRequest request = LogEventRequest.builder()
                .level("INFO")
                .message("Test message with metadata")
                .service("test-service")
                .metadata(metadata)
                .build();

        SendResult<String, String> sendResult = mock(SendResult.class);
        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition(TEST_TOPIC, 0), 0, 0, 0, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(TEST_TOPIC), anyString(), anyString())).thenReturn(future);

        // When
        LogEventResponse response = logIngestionService.ingestLog(request);

        // Then
        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        verify(kafkaTemplate).send(eq(TEST_TOPIC), anyString(), argThat(msg -> msg.contains("user_id")));
    }
}

