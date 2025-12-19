package com.loganomaly.detector.ingestion_service.controller;

import com.loganomaly.detector.common.dto.BatchLogRequest;
import com.loganomaly.detector.common.dto.BatchLogResponse;
import com.loganomaly.detector.common.dto.ErrorResponse;
import com.loganomaly.detector.common.dto.LogEventRequest;
import com.loganomaly.detector.common.dto.LogEventResponse;
import com.loganomaly.detector.ingestion_service.service.LogIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class LogController {

    private final LogIngestionService logIngestionService;

    @PostMapping
    public ResponseEntity<LogEventResponse> ingestLog(@RequestBody LogEventRequest request) {
        LogEventResponse response = logIngestionService.ingestLog(request);

        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchLogResponse> ingestBatch(@RequestBody BatchLogRequest request) {
        if (request.getLogs() == null || request.getLogs().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    BatchLogResponse.builder()
                            .acceptedCount(0)
                            .failedCount(0)
                            .build()
            );
        }

        BatchLogResponse response = logIngestionService.ingestBatch(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse error = ErrorResponse.builder()
                .error("Failed to process log event: " + e.getMessage())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(System.currentTimeMillis())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

