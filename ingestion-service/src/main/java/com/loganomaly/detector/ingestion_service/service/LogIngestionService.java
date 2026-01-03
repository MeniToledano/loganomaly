package com.loganomaly.detector.ingestion_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganomaly.detector.common.dto.BatchLogRequest;
import com.loganomaly.detector.common.dto.BatchLogResponse;
import com.loganomaly.detector.common.dto.LogEventRequest;
import com.loganomaly.detector.common.dto.LogEventResponse;
import com.loganomaly.detector.ingestion_service.util.InputSanitizer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class LogIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(LogIngestionService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final InputSanitizer inputSanitizer;

    @Value("${spring.kafka.topic.log-events}")
    private String logEventsTopic;

    public LogEventResponse ingestLog(LogEventRequest request) {
        String eventId = UUID.randomUUID().toString();

        // Sanitize input to prevent injection attacks
        sanitizeRequest(request);

        // Set timestamp if not provided
        if (request.getTimestamp() == null) {
            request.setTimestamp(Instant.now());
        }

        try {
            String message = objectMapper.writeValueAsString(new LogEventWithId(eventId, request));

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(logEventsTopic, eventId, message);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send log event {} to Kafka: {}", eventId, ex.getMessage());
                } else {
                    logger.debug("Successfully sent log event {} to Kafka, partition: {}, offset: {}",
                            eventId,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

            return LogEventResponse.builder()
                    .id(eventId)
                    .status("ACCEPTED")
                    .timestamp(Instant.now())
                    .build();

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize log event: {}", e.getMessage());
            return LogEventResponse.builder()
                    .id(eventId)
                    .status("FAILED")
                    .timestamp(Instant.now())
                    .build();
        }
    }

    public BatchLogResponse ingestBatch(BatchLogRequest request) {
        List<String> acceptedIds = new ArrayList<>();
        int failedCount = 0;

        for (LogEventRequest logEvent : request.getLogs()) {
            LogEventResponse response = ingestLog(logEvent);
            if ("ACCEPTED".equals(response.getStatus())) {
                acceptedIds.add(response.getId());
            } else {
                failedCount++;
            }
        }

        return BatchLogResponse.builder()
                .acceptedCount(acceptedIds.size())
                .failedCount(failedCount)
                .acceptedIds(acceptedIds)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Sanitize the request to prevent injection attacks
     */
    private void sanitizeRequest(LogEventRequest request) {
        request.setMessage(inputSanitizer.sanitize(request.getMessage()));
        request.setService(inputSanitizer.sanitizeServiceName(request.getService()));
        
        // Sanitize metadata values if present
        if (request.getMetadata() != null) {
            request.getMetadata().replaceAll((key, value) -> inputSanitizer.sanitize(value));
        }
    }

    /**
     * Wrapper class to include event ID in the Kafka message
     */
    private record LogEventWithId(String id, LogEventRequest event) {}
}

