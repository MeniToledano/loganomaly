package com.loganomaly.detector.analysis_service.service;

import com.loganomaly.detector.analysis_service.entity.Alert;
import com.loganomaly.detector.analysis_service.entity.LogEvent;
import com.loganomaly.detector.analysis_service.repository.AlertRepository;
import com.loganomaly.detector.analysis_service.repository.LogEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AnomalyDetectorService {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetectorService.class);

    private final LogEventRepository logEventRepository;
    private final AlertRepository alertRepository;

    @Value("${anomaly.error-threshold:5}")
    private int errorThreshold;

    @Value("${anomaly.time-window-minutes:1}")
    private int timeWindowMinutes;

    @Value("${anomaly.cooldown-minutes:5}")
    private int cooldownMinutes;

    /**
     * Analyze a log event for anomalies.
     * Currently implements: High Error Rate detection
     */
    @Transactional
    public void analyze(LogEvent event) {
        // Rule 1: High Error Rate - more than N errors from same service in time window
        if ("ERROR".equals(event.getLevel()) || "FATAL".equals(event.getLevel())) {
            checkHighErrorRate(event);
        }

        // Future rules can be added here:
        // - Security breach keywords detection
        // - Unusual activity patterns
        // - Service health degradation
    }

    /**
     * Check if error rate exceeds threshold for a service
     */
    private void checkHighErrorRate(LogEvent event) {
        Instant windowStart = Instant.now().minus(timeWindowMinutes, ChronoUnit.MINUTES);
        
        // Count recent errors for this service
        long errorCount = logEventRepository.countByLevelAndTimestampBetween(
                event.getLevel(),
                windowStart,
                Instant.now()
        );

        if (errorCount > errorThreshold) {
            // Check cooldown - don't create duplicate alerts
            Instant cooldownStart = Instant.now().minus(cooldownMinutes, ChronoUnit.MINUTES);
            boolean recentAlertExists = alertRepository.existsByTypeAndServiceAndDetectedAtAfter(
                    "HIGH_ERROR_RATE",
                    event.getService(),
                    cooldownStart
            );

            if (!recentAlertExists) {
                createHighErrorRateAlert(event, errorCount);
            } else {
                logger.debug("Skipping alert for {} - cooldown period active", event.getService());
            }
        }
    }

    /**
     * Create an alert for high error rate
     */
    private void createHighErrorRateAlert(LogEvent event, long errorCount) {
        Alert alert = Alert.builder()
                .type("HIGH_ERROR_RATE")
                .severity(determineSeverity(errorCount))
                .message(String.format(
                        "High error rate detected: %d %s events from service '%s' in the last %d minute(s)",
                        errorCount,
                        event.getLevel(),
                        event.getService(),
                        timeWindowMinutes
                ))
                .service(event.getService())
                .detectedAt(Instant.now())
                .build();

        Alert saved = alertRepository.save(alert);
        
        logger.warn("ALERT CREATED: {} - {} (severity: {})",
                saved.getType(),
                saved.getMessage(),
                saved.getSeverity());
    }

    /**
     * Determine alert severity based on error count
     */
    private String determineSeverity(long errorCount) {
        if (errorCount > errorThreshold * 3) {
            return "CRITICAL";
        } else if (errorCount > errorThreshold * 2) {
            return "WARNING";
        }
        return "INFO";
    }
}

