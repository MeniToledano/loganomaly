package com.loganomaly.detector.analysis_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "type", nullable = false, length = 50)
    private String type;  // e.g., "HIGH_ERROR_RATE", "SECURITY_BREACH"

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;  // INFO, WARNING, CRITICAL

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "service", nullable = false, length = 100)
    private String service;  // Which service triggered the alert

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "acknowledged", nullable = false)
    @Builder.Default
    private boolean acknowledged = false;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) {
            detectedAt = Instant.now();
        }
    }
}

