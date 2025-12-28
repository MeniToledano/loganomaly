package com.loganomaly.detector.analysis_service.controller;

import com.loganomaly.detector.analysis_service.entity.LogEvent;
import com.loganomaly.detector.analysis_service.repository.LogEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class LogEventController {

    private final LogEventRepository logEventRepository;

    /**
     * Get recent log events (last 100)
     */
    @GetMapping
    public ResponseEntity<List<LogEvent>> getRecentEvents() {
        return ResponseEntity.ok(logEventRepository.findTop100ByOrderByTimestampDesc());
    }

    /**
     * Get a specific log event by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<LogEvent> getEventById(@PathVariable UUID id) {
        return logEventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get log events by service name
     */
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<List<LogEvent>> getEventsByService(@PathVariable String serviceName) {
        return ResponseEntity.ok(logEventRepository.findByServiceOrderByTimestampDesc(serviceName));
    }

    /**
     * Get log events by level
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<List<LogEvent>> getEventsByLevel(@PathVariable String level) {
        return ResponseEntity.ok(logEventRepository.findByLevelOrderByTimestampDesc(level.toUpperCase()));
    }

    /**
     * Get log events within a time range
     */
    @GetMapping("/range")
    public ResponseEntity<List<LogEvent>> getEventsByTimeRange(
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end) {
        
        // Default to last 24 hours if not specified
        if (start == null) {
            start = Instant.now().minus(24, ChronoUnit.HOURS);
        }
        if (end == null) {
            end = Instant.now();
        }
        
        return ResponseEntity.ok(logEventRepository.findByTimestampBetweenOrderByTimestampDesc(start, end));
    }

    /**
     * Get count of log events (useful for health checks)
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getEventCount() {
        return ResponseEntity.ok(logEventRepository.count());
    }
}


