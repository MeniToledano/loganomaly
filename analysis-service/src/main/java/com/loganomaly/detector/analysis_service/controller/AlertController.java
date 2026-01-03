package com.loganomaly.detector.analysis_service.controller;

import com.loganomaly.detector.analysis_service.entity.Alert;
import com.loganomaly.detector.analysis_service.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AlertController {

    private final AlertRepository alertRepository;

    /**
     * Get all alerts (newest first)
     */
    @GetMapping
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(alertRepository.findAllByOrderByDetectedAtDesc());
    }

    /**
     * Get unacknowledged alerts only
     */
    @GetMapping("/unacknowledged")
    public ResponseEntity<List<Alert>> getUnacknowledgedAlerts() {
        return ResponseEntity.ok(alertRepository.findByAcknowledgedFalseOrderByDetectedAtDesc());
    }

    /**
     * Get alert by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Alert> getAlertById(@PathVariable UUID id) {
        return alertRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get alerts by severity (INFO, WARNING, CRITICAL)
     */
    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<Alert>> getAlertsBySeverity(@PathVariable String severity) {
        return ResponseEntity.ok(alertRepository.findBySeverityOrderByDetectedAtDesc(severity.toUpperCase()));
    }

    /**
     * Get alerts by service name
     */
    @GetMapping("/service/{service}")
    public ResponseEntity<List<Alert>> getAlertsByService(@PathVariable String service) {
        return ResponseEntity.ok(alertRepository.findByServiceOrderByDetectedAtDesc(service));
    }

    /**
     * Get recent alerts (last N hours, default 24)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Alert>> getRecentAlerts(
            @RequestParam(defaultValue = "24") int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return ResponseEntity.ok(alertRepository.findByDetectedAtAfterOrderByDetectedAtDesc(since));
    }

    /**
     * Get alert statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAlertStats() {
        return ResponseEntity.ok(Map.of(
                "total", alertRepository.count(),
                "unacknowledged", alertRepository.countByAcknowledgedFalse()
        ));
    }

    /**
     * Acknowledge an alert
     */
    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<Alert> acknowledgeAlert(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "system") String acknowledgedBy) {
        
        return alertRepository.findById(id)
                .map(alert -> {
                    alert.setAcknowledged(true);
                    alert.setAcknowledgedAt(Instant.now());
                    alert.setAcknowledgedBy(acknowledgedBy);
                    return ResponseEntity.ok(alertRepository.save(alert));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

