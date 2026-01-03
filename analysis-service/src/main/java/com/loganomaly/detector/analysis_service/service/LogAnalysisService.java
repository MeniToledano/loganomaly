package com.loganomaly.detector.analysis_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganomaly.detector.analysis_service.entity.LogEvent;
import com.loganomaly.detector.analysis_service.repository.LogEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LogAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(LogAnalysisService.class);

    private final LogEventRepository logEventRepository;
    private final ObjectMapper objectMapper;
    private final AnomalyDetectorService anomalyDetector;

    @KafkaListener(
            topics = "${spring.kafka.topic.log-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consumeLogEvent(String message) {
        logger.debug("Received log event from Kafka: {}", message);

        try {
            LogEvent logEvent = parseLogEvent(message);
            LogEvent saved = logEventRepository.save(logEvent);
            
            logger.info("Stored log event: id={}, service={}, level={}, message={}",
                    saved.getId(),
                    saved.getService(),
                    saved.getLevel(),
                    truncateMessage(saved.getMessage(), 100));

            // Run anomaly detection on the saved event
            anomalyDetector.analyze(saved);

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse log event JSON: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to process log event: {}", e.getMessage(), e);
        }
    }

    /**
     * Parse the Kafka message into a LogEvent entity.
     * The message format is: {"id": "...", "event": {...}}
     */
    private LogEvent parseLogEvent(String message) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(message);
        
        String id = root.get("id").asText();
        JsonNode eventNode = root.get("event");

        // Parse timestamp - handle epoch seconds (number) or ISO string
        Instant timestamp;
        if (eventNode.has("timestamp") && !eventNode.get("timestamp").isNull()) {
            JsonNode timestampNode = eventNode.get("timestamp");
            if (timestampNode.isNumber()) {
                // Epoch seconds (with possible fractional nanoseconds)
                double epochSeconds = timestampNode.asDouble();
                long seconds = (long) epochSeconds;
                long nanos = (long) ((epochSeconds - seconds) * 1_000_000_000);
                timestamp = Instant.ofEpochSecond(seconds, nanos);
            } else {
                // ISO-8601 string
                timestamp = Instant.parse(timestampNode.asText());
            }
        } else {
            timestamp = Instant.now();
        }

        // Parse metadata
        Map<String, String> metadata = new HashMap<>();
        if (eventNode.has("metadata") && !eventNode.get("metadata").isNull()) {
            eventNode.get("metadata").fields().forEachRemaining(entry ->
                    metadata.put(entry.getKey(), entry.getValue().asText())
            );
        }

        return LogEvent.builder()
                .id(UUID.fromString(id))
                .timestamp(timestamp)
                .level(eventNode.get("level").asText())
                .message(eventNode.get("message").asText())
                .service(eventNode.get("service").asText())
                .metadata(metadata.isEmpty() ? null : metadata)
                .build();
    }

    /**
     * Truncate message for logging purposes
     */
    private String truncateMessage(String message, int maxLength) {
        if (message == null) {
            return "";
        }
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }
}


