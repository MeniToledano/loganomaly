package com.loganomaly.detector.ingestion_service.controller;

import com.loganomaly.detector.common.dto.BatchLogRequest;
import com.loganomaly.detector.common.dto.BatchLogResponse;
import com.loganomaly.detector.common.dto.ErrorResponse;
import com.loganomaly.detector.common.dto.LogEventRequest;
import com.loganomaly.detector.common.dto.LogEventResponse;
import com.loganomaly.detector.ingestion_service.service.LogIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class LogController {

    private final LogIngestionService logIngestionService;

    @PostMapping
    public ResponseEntity<LogEventResponse> ingestLog(@Valid @RequestBody LogEventRequest request) {
        LogEventResponse response = logIngestionService.ingestLog(request);

        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchLogResponse> ingestBatch(@Valid @RequestBody BatchLogRequest request) {
        BatchLogResponse response = logIngestionService.ingestBatch(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        ErrorResponse error = ErrorResponse.builder()
                .error("Validation failed: " + errors)
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(System.currentTimeMillis())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
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

